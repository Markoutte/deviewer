// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.scroll;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.util.LinkedList;
import java.util.List;

/**
 * The utility class that helps to avoid accidental scroll in perpendicular direction.
 */
@ApiStatus.Internal
public final class LatchingScroll {

    private final List<MyScrollEvent> myScrollEvents = new LinkedList<>();

    /**
     * Checks, if current event should be ignored.
     *
     * The class tracks all events which are passed into this method,
     * and calculates, if current one should be ignored. With some expire time
     * previous events are removed from tracker.
     */
    public boolean shouldBeIgnored(MouseWheelEvent event) {
        myScrollEvents.add(new MyScrollEvent(event.getWhen(), event.getPreciseWheelRotation(), event.isShiftDown()));
        double xs = 0.0, ys = 0.0;

        var iterator = myScrollEvents.iterator();
        while (iterator.hasNext()) {
            MyScrollEvent se = iterator.next();
            if (se.when() + getExpireAfter() < event.getWhen()) {
                iterator.remove();
            } else {
                if (se.isHorizontal()) {
                    xs += Math.abs(se.rotation());
                } else {
                    ys += Math.abs(se.rotation());
                }
            }
        }

        double angle = Math.toDegrees(Math.atan(Math.abs(ys / xs)));
        boolean isHorizontal = event.isShiftDown();

        double thatAngle = getAngle();
        if (angle <= thatAngle && !isHorizontal || angle >= 90 - thatAngle && isHorizontal) {
            return true;
        }

        return false;
    }

    private static @Nullable Component getViewportView(@NotNull JScrollPane pane) {
        JViewport viewport = pane.getViewport();
        if (viewport != null) {
            return viewport.getView();
        }
        return null;
    }

    private static long getExpireAfter() {
        return 200;
    }

    private static double getAngle() {
        return 45.0;
    }

    public static boolean isEnabled() {
        return true;
    }

    private record MyScrollEvent(long when, double rotation, boolean isHorizontal) {
    }
}