package org.jupiter.common.util.internal;

import org.junit.Test;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ALL")
public class RecyclerTest {

    @Test(expected = Exception.class)
    public void testMultipleRecycle() {
        RecyclableObject object = RecyclableObject.newInstance();
        object.recycle();
        object.recycle();
    }

    @Test
    public void testRecycle() {
        RecyclableObject object = RecyclableObject.newInstance();
        object.recycle();
        RecyclableObject object2 = RecyclableObject.newInstance();
        assertSame(object, object2);
        object2.recycle();
    }

    static final class RecyclableObject {

        private static final Recyclers<RecyclableObject> recycler = new Recyclers<RecyclableObject>() {

            @Override
            protected RecyclableObject newObject(Handle<RecyclableObject> handle) {
                return new RecyclableObject(handle);
            }
        };

        private final Recyclers.Handle<RecyclableObject> handle;

        private RecyclableObject(Recyclers.Handle<RecyclableObject> handle) {
            this.handle = handle;
        }

        public static RecyclableObject newInstance() {
            return recycler.get();
        }

        public void recycle() {
            recycler.recycle(this, handle);
        }
    }

    @Test
    public void testMaxCapacity() {
        testMaxCapacity(300);
        Random rand = new Random();
        for (int i = 0; i < 50; i++) {
            testMaxCapacity(rand.nextInt(1000) + 256); // 256 - 1256
        }
    }

    void testMaxCapacity(int maxCapacity) {
        Recyclers<HandledObject> recycler = new Recyclers<HandledObject>(maxCapacity) {

            @Override
            protected HandledObject newObject(
                    Recyclers.Handle<HandledObject> handle) {
                return new HandledObject(handle);
            }
        };

        HandledObject[] objects = new HandledObject[maxCapacity * 3];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = recycler.get();
        }

        for (int i = 0; i < objects.length; i++) {
            recycler.recycle(objects[i], objects[i].handle);
            objects[i] = null;
        }

        assertEquals(maxCapacity, recycler.threadLocalCapacity());
    }

    @Test
    public void testRecycleAtDifferentThread() throws Exception {
        final Recyclers<HandledObject> recycler = new Recyclers<HandledObject>(256) {

            @Override
            protected HandledObject newObject(Recyclers.Handle<HandledObject> handle) {

                return new HandledObject(handle);
            }
        };

        final HandledObject o = recycler.get();
        final Thread thread = new Thread() {
            @Override
            public void run() {
                recycler.recycle(o, o.handle);
            }
        };
        thread.start();
        thread.join();

        assertThat(recycler.get(), is(sameInstance(o)));
    }

    @Test
    public void testMaxCapacityWithRecycleAtDifferentThread() throws Exception {
        final int maxCapacity = 4; // Choose the number smaller than WeakOrderQueue.LINK_CAPACITY
        final Recyclers<HandledObject> recycler = new Recyclers<HandledObject>(maxCapacity) {

            @Override
            protected HandledObject newObject(Recyclers.Handle<HandledObject> handle) {
                return new HandledObject(handle);
            }
        };

        // Borrow 2 * maxCapacity objects.
        // Return the half from the same thread.
        // Return the other half from the different thread.

        final HandledObject[] array = new HandledObject[maxCapacity * 3];
        for (int i = 0; i < array.length; i++) {
            array[i] = recycler.get();
        }

        for (int i = 0; i < maxCapacity; i++) {
            recycler.recycle(array[i], array[i].handle);
        }

        final Thread thread = new Thread() {

            @Override
            public void run() {
                for (int i = maxCapacity; i < array.length; i++) {
                    recycler.recycle(array[i], array[i].handle);
                }
            }
        };
        thread.start();
        thread.join();

        assertThat(recycler.threadLocalCapacity(), is(maxCapacity));
        assertThat(recycler.threadLocalSize(), is(maxCapacity));

        for (int i = 0; i < array.length; i++) {
            recycler.get();
        }

        assertThat(recycler.threadLocalCapacity(), is(maxCapacity));
        assertThat(recycler.threadLocalSize(), is(0));
    }

    static final class HandledObject {
        Recyclers.Handle<HandledObject> handle;

        HandledObject(Recyclers.Handle<HandledObject> handle) {
            this.handle = handle;
        }
    }
}