/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

package com.jogamp.newt.impl.x11;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;

public class X11Window extends Window {
    private static final String WINDOW_CLASS_NAME = "NewtWindow";

    static {
        X11Display.initSingleton();
    }

    public X11Window() {
    }

    protected void createNativeImpl() {
        X11Screen screen = (X11Screen) getScreen();
        X11Display display = (X11Display) screen.getDisplay();
        config = GraphicsConfigurationFactory.getFactory(display.getGraphicsDevice()).chooseGraphicsConfiguration(caps, null, screen.getGraphicsScreen());
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        attachedToParent = 0 != parentWindowHandle ;
        X11GraphicsConfiguration x11config = (X11GraphicsConfiguration) config;
        long visualID = x11config.getVisualID();
        long w = CreateWindow0(parentWindowHandle, 
                              display.getHandle(), screen.getIndex(), visualID, 
                              display.getJavaObjectAtom(), display.getWindowDeleteAtom(), 
                              x, y, width, height, undecorated());
        if (w == 0 || w!=windowHandle) {
            throw new NativeWindowException("Error creating window: "+w);
        }
        windowHandleClose = windowHandle;
        displayHandleClose = display.getHandle();
    }

    protected void closeNative() {
        if(0!=displayHandleClose && 0!=windowHandleClose && null!=getScreen() ) {
            X11Display display = (X11Display) getScreen().getDisplay();
            CloseWindow0(displayHandleClose, windowHandleClose, display.getJavaObjectAtom());
            windowHandleClose = 0;
            displayHandleClose = 0;
        }
    }

    protected void windowDestroyed() {
        windowHandleClose = 0;
        displayHandleClose = 0;
        super.windowDestroyed();
    }

    protected void setVisibleImpl() {
        setVisible0(getDisplayHandle(), windowHandle, visible);
        clearEventMask();
    }

    public void setSize(int width, int height) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("X11Window setSize: "+this.width+"x"+this.height+" -> "+width+"x"+height+", fs "+fullscreen+", windowHandle "+windowHandle);
        }
        if (width != this.width || this.height != height) {
            if(!fullscreen) {
                nfs_width=width;
                nfs_height=height;
                if(0!=windowHandle) {
                    // this width/height will be set by windowChanged, called by X11
                    setSize0(getDisplayHandle(), windowHandle, width, height);
                } else {
                    this.width = width;
                    this.height = height;
                }
            }
        }
    }

    public void setPosition(int x, int y) {
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("X11Window setPosition: "+this.x+"/"+this.y+" -> "+x+"/"+y+", fs "+fullscreen+", windowHandle "+windowHandle);
        }
        if ( this.x != x || this.y != y ) {
            if(!fullscreen) {
                nfs_x=x;
                nfs_y=y;
                if(0!=windowHandle) {
                    // this x/y will be set by windowChanged, called by X11
                    setPosition0(parentWindowHandle, getDisplayHandle(), windowHandle, x, y);
                } else {
                    this.x = x;
                    this.y = y;
                }
            }
        }
    }

    public boolean setFullscreen(boolean fullscreen) {
        if(0!=windowHandle && this.fullscreen!=fullscreen) {
            int x,y,w,h;
            this.fullscreen=fullscreen;
            if(fullscreen) {
                x = 0; y = 0;
                w = screen.getWidth();
                h = screen.getHeight();
            } else {
                x = nfs_x;
                y = nfs_y;
                w = nfs_width;
                h = nfs_height;
            }
            if(DEBUG_IMPLEMENTATION || DEBUG_WINDOW_EVENT) {
                System.err.println("X11Window fs: "+fullscreen+" "+x+"/"+y+" "+w+"x"+h+", "+undecorated());
            }
            setPosSizeDecor0(fullscreen?0:parentWindowHandle, getDisplayHandle(), getScreenIndex(), windowHandle, x, y, w, h, undecorated());
        }
        return fullscreen;
    }

    final boolean undecorated() { return attachedToParent || undecorated || fullscreen ; }

    // @Override
    public void requestFocus() {
        super.requestFocus();
        if (windowHandle != 0L) {
            requestFocus0(getDisplayHandle(), windowHandle);
        }
    }

    // @Override
    public void setTitle(String title) {
        if (title == null) {
            title = "";
        }
        if (0!=windowHandle && !title.equals(getTitle())) {
            super.setTitle(title);
            setTitle0(getDisplayHandle(), windowHandle, title);
        }
    }


    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs0();
    private        native long CreateWindow0(long parentWindowHandle, long display, int screen_index, 
                                            long visualID, long javaObjectAtom, long windowDeleteAtom, 
                                            int x, int y, int width, int height, boolean undecorated);
    private        native void CloseWindow0(long display, long windowHandle, long javaObjectAtom);
    private        native void setVisible0(long display, long windowHandle, boolean visible);
    private        native void setSize0(long display, long windowHandle, int width, int height);
    private        native void setPosSizeDecor0(long parentWindowHandle, long display, int screen_index, long windowHandle, 
                                                int x, int y, int width, int height, boolean undecorated);
    private        native void setTitle0(long display, long windowHandle, String title);
    private        native void requestFocus0(long display, long windowHandle);
    private        native void setPosition0(long parentWindowHandle, long display, long windowHandle, int x, int y);

    private void windowChanged(int newX, int newY, int newWidth, int newHeight) {
        if(width != newWidth || height != newHeight) {
            if(DEBUG_IMPLEMENTATION) {
                System.err.println("X11Window windowChanged size: "+this.width+"x"+this.height+" -> "+newWidth+"x"+newHeight);
            }
            width = newWidth;
            height = newHeight;
            if(!fullscreen) {
                nfs_width=width;
                nfs_height=height;
            }
            sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
        }
        if( 0==parentWindowHandle && ( x != newX || y != newY ) ) {
            if(DEBUG_IMPLEMENTATION) {
                System.err.println("X11Window windowChanged position: "+this.x+"/"+this.y+" -> "+newX+"x"+newY);
            }
            x = newX;
            y = newY;
            if(!fullscreen) {
                nfs_x=x;
                nfs_y=y;
            }
            sendWindowEvent(WindowEvent.EVENT_WINDOW_MOVED);
        }
    }

    /**
     * @param focusGained
     */
    private void focusChanged(boolean focusGained) {
        if (focusGained) {
            sendWindowEvent(WindowEvent.EVENT_WINDOW_GAINED_FOCUS);
        } else {
            sendWindowEvent(WindowEvent.EVENT_WINDOW_LOST_FOCUS);
        }
    }

    private void windowCreated(long windowHandle) {
        this.windowHandle = windowHandle;
    }

    private long   windowHandleClose;
    private long   displayHandleClose;
    private boolean attachedToParent;

    // non fullscreen dimensions ..
    private int nfs_width, nfs_height, nfs_x, nfs_y;
}
