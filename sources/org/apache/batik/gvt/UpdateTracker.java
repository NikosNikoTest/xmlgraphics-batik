/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.gvt;

import java.awt.geom.Rectangle2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import java.lang.ref.WeakReference;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.batik.ext.awt.image.renderable.Filter;
import org.apache.batik.gvt.event.GraphicsNodeChangeAdapter;
import org.apache.batik.gvt.event.GraphicsNodeChangeEvent;

/**
 * This class tracks the changes on a GVT tree
 *
 * @author <a href="mailto:Thomas.DeWeeese@Kodak.com">Thomas DeWeese</a>
 * @version $Id$
 */
public class UpdateTracker extends GraphicsNodeChangeAdapter {

    Map dirtyNodes = null;
    Map nodeBounds = new HashMap();

    public UpdateTracker(){
    }
    
    /**
     * Tells whether the GVT tree has changed.
     */
    public boolean hasChanged() {
        return (dirtyNodes != null);
    }

    /**
     * Returns the list of dirty areas on GVT.
     */
    public List getDirtyAreas() {
        // System.out.println("Getting dirty areas");

        if (dirtyNodes == null) 
            return null;

        List ret = new LinkedList();
        Set keys = dirtyNodes.keySet();
        Iterator i = keys.iterator();
        while (i.hasNext()) {
            WeakReference gnWRef = (WeakReference)i.next();
            GraphicsNode  gn     = (GraphicsNode)gnWRef.get();

            // if the weak ref has been cleared then this node is no
            // longer part of the GVT tree (and the change should be
            // reflected in some ancestor that should also be in the
            // dirty list).
            if (gn == null) continue;

            AffineTransform oat;
            oat = (AffineTransform)dirtyNodes.get(gnWRef);
            
            Rectangle2D srcORgn = (Rectangle2D)nodeBounds.get(gnWRef);

            Rectangle2D srcNRgn = gn.getBounds();
            AffineTransform nat = gn.getTransform();
            nodeBounds.put(gnWRef, srcNRgn); // remember the new bounds...
            // System.out.println("Old: " + srcORgn);
            // System.out.println("New: " + srcNRgn);
            Shape oRgn = srcORgn;
            Shape nRgn = srcNRgn;

            do {
                Filter f;
                f = gn.getGraphicsNodeRable(false);
                // f.invalidateCache(oRng);
                // f.invalidateCache(nRng);

                f = gn.getEnableBackgroundGraphicsNodeRable(false);
                // (need to push rgn through filter chain if any...)
                // f.invalidateCache(oRng);
                // f.invalidateCache(nRng);

                gn = gn.getParent();
                if (gn == null) break;
                if (dirtyNodes.get(gn.getWeakReference()) != null) break;

                AffineTransform at = gn.getTransform();

                if (oat != null){
                    oRgn = oat.createTransformedShape(srcORgn);
                    if (at != null){
                        oat.preConcatenate(at);
                    }
                } else {
                    oat = at;
                }
                if (nat != null){
                    nRgn = nat.createTransformedShape(srcNRgn);
                    if (at != null){
                        nat.preConcatenate(at);
                    }
                } else {
                    nat = at;
                }

            } while (true);

            if (gn == null) {
                // We made it to the root graphics node so add them.
                ret.add(oRgn);
                ret.add(nRgn);
            }
        }

        // System.out.println("Dirty area: " + ret);
        return ret;
    }

    /**
     * Recieves notification of a change to a GraphicsNode.
     * @param gn The graphics node that is changing.
     */
    public void changeStarted(GraphicsNodeChangeEvent gnce) {
        // System.out.println("A node has changed for: " + this);
        GraphicsNode gn = gnce.getGraphicsNode();
        WeakReference gnWRef = gn.getWeakReference();

        if (dirtyNodes == null) {
            dirtyNodes = new HashMap();
            dirtyNodes.put(gnWRef, gn.getTransform());
        } else {
            Object o = dirtyNodes.get(gnWRef);
            if (o == null)
                dirtyNodes.put(gnWRef, gn.getTransform());
        }

        Object o = nodeBounds.get(gnWRef);
        while (o == null) {
            nodeBounds.put(gnWRef, gn.getBounds());
            gn = gn.getParent();
            if (gn == null) break;
            gnWRef = gn.getWeakReference();
            o = nodeBounds.get(gnWRef);
        }
    }

    /**
     * Clears the tracker.
     */
    public void clear() {
        dirtyNodes = null;
    }

    public static class DirtyInfo {
        // Always references a GraphicsNode.
        WeakReference   gn;

        // The transform from gn to parent at time of construction.
        AffineTransform gn2parent;

        public DirtyInfo(GraphicsNode gn, AffineTransform at) {
            this.gn     = gn.getWeakReference();
            this.gn2parent = at;
        }

        public GraphicsNode getGraphicsNode() {
            return (GraphicsNode)gn.get();
        }

        public AffineTransform getGn2Parent() {
            return gn2parent;
        }
    }
}