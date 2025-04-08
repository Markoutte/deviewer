// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.animation;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public class Animations {

    /**
     * Update animations' delay time in such a way that
     * animations will be run one by one.
     */
    public static Collection<Animation> makeSequent(Animation... animations) {
        for (int i = 1; i < animations.length; i++) {
            Animation prev = animations[i - 1];
            Animation curr = animations[i];
            curr.setDelay(curr.getDelay() + prev.getDelay() + prev.getDuration());
        }
        Collection<Animation> result = new ArrayList<>();
        for (Animation animation : animations) {
            result.add(animation);
        }
        return result;
    }

    /**
     * Empty animation (do nothing).
     *
     * May be used as an anchor frame for any of Animation.runWhenScheduled, Animation.runWhenUpdated or Animation.runWhenExpired methods.
     */
    public static Animation animation() {
        return new Animation();
    }

    /**
     * Very common animation.
     */
    public static Animation animation(DoubleConsumer consumer) {
        return new Animation(consumer);
    }

    public static Animation animation(int from, int to, IntConsumer consumer) {
        return new Animation(value -> consumer.accept((int) Math.round(from + value * (to - from))));
    }

    public static <T> Animation animation(AnimationContext<T> context, DoubleFunction<T> function) {
        return Animation.withContext(context, function);
    }

    public static Animation animation(long from, long to, LongConsumer consumer) {
        return new Animation(value -> consumer.accept(Math.round(from + value * (to - from))));
    }

    public static Animation animation(double from, double to, DoubleConsumer consumer) {
        return new Animation(value -> consumer.accept(from + value * (to - from)));
    }

    public static Animation animation(Point from, Point to, Consumer<Point> consumer) {
        return new Animation(new DoublePointFunction(from, to), consumer);
    }

    public static Animation animation(Rectangle from, Rectangle to, Consumer<Rectangle> consumer) {
        return new Animation(new DoubleRectangleFunction(from, to), consumer);
    }

    public static Animation animation(Dimension from, Dimension to, Consumer<Dimension> consumer) {
        return new Animation(new DoubleDimensionFunction(from, to), consumer);
    }

    public static Animation animation(Color from, Color to, Consumer<Color> consumer) {
        return new Animation(new DoubleColorFunction(from, to), consumer);
    }

//    public static Animation transparent(Color color, Consumer<Color> consumer) {
//        return animation(color, ColorUtil.withAlpha(color, 0.0), consumer);
//    }

    public static <T> DoubleConsumer consumer(DoubleFunction<T> function, Consumer<T> consumer) {
        return value -> consumer.accept(function.apply(value));
    }

    private static DoubleFunction<String> text(String from, String to) {
        String shorter = from.length() < to.length() ? from : to;
        String longer = shorter.equals(from) ? to : from;

        if (shorter.length() == longer.length() || !longer.startsWith(shorter)) {
            double fraction = (double) from.length() / (from.length() + to.length());
            return timeline -> {
                if (timeline < fraction) {
                    int length = (int) Math.round(from.length() * ((fraction - timeline) / fraction));
                    return from.substring(0, length);
                } else {
                    int length = (int) Math.round(to.length() * ((timeline - fraction) / (1 - fraction)));
                    return to.substring(0, length);
                }
            };
        }

        if (shorter.equals(from)) {
            return timeline -> longer.substring(0,
                    (int) Math.round(shorter.length() + (longer.length() - shorter.length()) * timeline));
        } else {
            return timeline -> longer.substring(0,
                    (int) Math.round(longer.length() - (longer.length() - shorter.length()) * timeline));
        }
    }

    public static Animation animation(String from, String to, Consumer<String> consumer) {
        return new Animation(text(from, to), consumer);
    }

    private static DoubleIntFunction range(int from, int to) {
        return value -> (int) Math.round(from + value * (to - from));
    }

    private interface DoubleIntFunction {
        int apply(double value);
    }

    public static class DoubleColorFunction implements DoubleFunction<Color> {

        private final DoubleIntFunction red;
        private final DoubleIntFunction green;
        private final DoubleIntFunction blue;
        private final DoubleIntFunction alpha;

        public DoubleColorFunction(Color from, Color to) {
            this.red = range(from.getRed(), to.getRed());
            this.green = range(from.getGreen(), to.getGreen());
            this.blue = range(from.getBlue(), to.getBlue());
            this.alpha = range(from.getAlpha(), to.getAlpha());
        }

        @Override
        public Color apply(double value) {
            return new Color(
                    red.apply(value),
                    green.apply(value),
                    blue.apply(value),
                    alpha.apply(value)
            );
        }
    }

    public static class DoublePointFunction implements DoubleFunction<Point> {

        private final DoubleIntFunction x;
        private final DoubleIntFunction y;

        public DoublePointFunction(Point from, Point to) {
            this.x = range(from.x, to.x);
            this.y = range(from.y, to.y);
        }

        @Override
        public Point apply(double value) {
            return new Point(x.apply(value), y.apply(value));
        }
    }

    public static class DoubleDimensionFunction implements DoubleFunction<Dimension> {

        private final DoubleIntFunction width;
        private final DoubleIntFunction height;

        public DoubleDimensionFunction(Dimension from, Dimension to) {
            this.width = range(from.width, to.width);
            this.height = range(from.height, to.height);
        }

        @Override
        public Dimension apply(double value) {
            return new Dimension(width.apply(value), height.apply(value));
        }
    }

    public static class DoubleRectangleFunction implements DoubleFunction<Rectangle> {

        private final DoubleIntFunction x;
        private final DoubleIntFunction y;
        private final DoubleIntFunction width;
        private final DoubleIntFunction height;

        public DoubleRectangleFunction(Rectangle from, Rectangle to) {
            this.x = range(from.x, to.x);
            this.y = range(from.y, to.y);
            this.width = range(from.width, to.width);
            this.height = range(from.height, to.height);
        }

        @Override
        public Rectangle apply(double value) {
            return new Rectangle(
                    x.apply(value),
                    y.apply(value),
                    width.apply(value),
                    height.apply(value)
            );
        }
    }
}
