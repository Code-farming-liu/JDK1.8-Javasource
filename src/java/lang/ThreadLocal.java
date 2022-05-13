/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.lang;
import java.lang.ref.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * This class provides thread-local variables.  These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * {@code get} or {@code set} method) has its own, independently initialized
 * copy of the variable.  {@code ThreadLocal} instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
 *这个类提供线程局部变量。这些变量与正常的变量不同，每个线程访问一个(通过它的get或set方法)都有它自己的、
 * 独立初始化的变量副本。ThreadLocal实例通常是类中的私有静态字段，希望将状态与线程关联(例如，用户ID或事务ID)。
 * <p>For example, the class below generates unique identifiers local to each
 * thread.
 * A thread's id is assigned the first time it invokes {@code ThreadId.get()}
 * and remains unchanged on subsequent calls.
 * <pre>
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 *下面ThreadId类会在每个线程中生成唯一标识符。
 * 线程的id在第一次调用threadid.get()时被分配，在随后的调用中保持不变。
 *
 *  ThreadId类利用AtomicInteger原子方法getAndIncrement，
 *  为每个线程创建一个threadId变量，例如第一个线程是1，
 *  第二个线程是2...，并提供一个类静态get方法用以获取当前线程ID。：
 *  用户可以自定义initialValue()初始化方法，来初始化threadLocal的值。
 *
 * public class ThreadId {
 *     // Atomic integer containing the next thread ID to be assigned
 *     private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *     // Thread local variable containing each thread's ID
 *     private static final ThreadLocal&lt;Integer&gt; threadId =
 *         new ThreadLocal&lt;Integer&gt;() {
 *             &#64;Override protected Integer initialValue() {
 *                 return nextId.getAndIncrement();
 *         }
 *     };
 *
 *     // Returns the current thread's unique ID, assigning it if necessary
 *     public static int get() {
 *         return threadId.get();
 *     }
 * }
 * </pre>
 * <p>Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the {@code ThreadLocal}
 * instance is accessible; after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist).
 *
 * @author  Josh Bloch and Doug Lea
 * @since   1.2
 */
public class ThreadLocal<T> {
    /**
     * ThreadLocals rely on per-thread linear-probe hash maps attached
     * to each thread (Thread.threadLocals and
     * inheritableThreadLocals).  The ThreadLocal objects act as keys,
     * searched via threadLocalHashCode.  This is a custom hash code
     * (useful only within ThreadLocalMaps) that eliminates collisions
     * in the common case where consecutively constructed ThreadLocals
     * are used by the same threads, while remaining well-behaved in
     * less common cases.
     */
    //生成hash code间隙为这个魔数，
    // 可以让生成出来的值或者说ThreadLocal的ID较为均匀地分布在2的幂大小的数组中。
    private final int threadLocalHashCode = nextHashCode();

    /**
     * The next hash code to be given out. Updated atomically. Starts at
     * zero.
     */
    private static AtomicInteger nextHashCode =
        new AtomicInteger();

    /**
     * The difference between successively generated hash codes - turns
     * implicit sequential thread-local IDs into near-optimally spread
     * multiplicative hash values for power-of-two-sized tables.
     */
    //threadLocalHashCode的基础上加上一个魔数0x61c88647的。这个魔数的选取与斐波那契散列有关
    private static final int HASH_INCREMENT = 0x61c88647;

    /**
     * Returns the next hash code.
     */
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * Returns the current thread's "initial value" for this
     * thread-local variable.  This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, unless the thread previously invoked the {@link #set}
     * method, in which case the {@code initialValue} method will not
     * be invoked for the thread.  Normally, this method is invoked at
     * most once per thread, but it may be invoked again in case of
     * subsequent invocations of {@link #remove} followed by {@link #get}.
     *
     * <p>This implementation simply returns {@code null}; if the
     * programmer desires thread-local variables to have an initial
     * value other than {@code null}, {@code ThreadLocal} must be
     * subclassed, and this method overridden.  Typically, an
     * anonymous inner class will be used.
     *
     * @return the initial value for this thread-local
     */
    protected T initialValue() {
        return null;
    }

    /**
     * Creates a thread local variable. The initial value of the variable is
     * determined by invoking the {@code get} method on the {@code Supplier}.
     *
     * @param <S> the type of the thread local's value
     * @param supplier the supplier to be used to determine the initial value
     * @return a new thread local variable
     * @throws NullPointerException if the specified supplier is null
     * @since 1.8
     */
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    /**
     * Creates a thread local variable.
     * @see #withInitial(java.util.function.Supplier)
     */
    public ThreadLocal() {
    }

    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        //获得当前的线程
        //同set方法类似获取对应线程中的ThreadLocalMap实例
        Thread t = Thread.currentThread();
        //返回一个ThreadLocalMap对象
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        //为空返回初始化值
        return setInitialValue();
    }

    /**
     * Variant of set() to establish initialValue. Used instead
     * of set() in case user has overridden the set() method.
     *
     * @return the initial value
     */

    private T setInitialValue() {
        //获取初始化值，默认为null(如果没有子类进行覆盖)
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        //不为空不用再初始化，直接调用set操作设值
        if (map != null)
            map.set(this, value);
        else
            //第一次初始化，createMap在上面介绍set()的时候有介绍过。
            createMap(t, value);
        return value;
    }

    /**
     * Sets the current thread's copy of this thread-local variable
     * to the specified value.  Most subclasses will have no need to
     * override this method, relying solely on the {@link #initialValue}
     * method to set the values of thread-locals.
     *
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    /**
     代码很简单，获取当前线程，并获取当前线程的ThreadLocalMap实例（从getMap(Thread t)中很容易看出来）。
     如果获取到的map实例不为空，调用map.set()方法，
     否则调用构造函数 ThreadLocal.ThreadLocalMap(this, firstValue)实例化map。
     **/
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }

    /**
     * Removes the current thread's value for this thread-local
     * variable.  If this thread-local variable is subsequently
     * {@linkplain #get read} by the current thread, its value will be
     * reinitialized by invoking its {@link #initialValue} method,
     * unless its value is {@linkplain #set set} by the current thread
     * in the interim.  This may result in multiple invocations of the
     * {@code initialValue} method in the current thread.
     *
     * @since 1.5
     */
     public void remove() {
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             m.remove(this);
     }

    /**
     * Get the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param  t the current thread
     * @return the map
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    /**
     * Create the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the map
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    /**
     * Factory method to create map of inherited thread locals.
     * Designed to be called only from Thread constructor.
     *
     * @param  parentMap the map associated with parent thread
     * @return a map containing the parent's inheritable bindings
     */
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * Method childValue is visibly defined in subclass
     * InheritableThreadLocal, but is internally defined here for the
     * sake of providing createInheritedMap factory method without
     * needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding
     * instanceof tests in methods.
     */
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * An extension of ThreadLocal that obtains its initial value from
     * the specified {@code Supplier}.
     */
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }

    /**
     * ThreadLocalMap is a customized hash map suitable only for
     * maintaining thread local values. No operations are exported
     * outside of the ThreadLocal class. The class is package private to
     * allow declaration of fields in class Thread.  To help deal with
     * very large and long-lived usages, the hash table entries use
     * WeakReferences for keys. However, since reference queues are not
     * used, stale entries are guaranteed to be removed only when
     * the table starts running out of space.
     */
    static class ThreadLocalMap {

        /**
         * The entries in this hash map extend WeakReference, using
         * its main ref field as the key (which is always a
         * ThreadLocal object).  Note that null keys (i.e. entry.get()
         * == null) mean that the key is no longer referenced, so the
         * entry can be expunged from table.  Such entries are referred to
         * as "stale entries" in the code that follows.
         */

        /**
         * Entry继承WeakReference，并且用ThreadLocal作为key.如果key为null
         * (entry.get() == null)表示key不再被引用，表示ThreadLocal对象被回收
         * 因此这时候entry也可以从table从清除。
         */

        static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        /**
         * The initial capacity -- MUST be a power of two.
         * 初始容量 —— 必须是2的冥
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * 存放数据的table，Entry类的定义在下面分析
         * 同样，数组长度必须是2的冥。
         */
        private Entry[] table;


        /**
         * 数组里面entrys的个数，可以用于判断table当前使用量是否超过负因子。
         */
        private int size = 0;

        /**
         * 进行扩容的阈值，表使用量大于它的时候进行扩容。
         */
        private int threshold; // Default to 0

        /**
         * 定义为长度的2/3
         */
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        /**
         * Increment i modulo len.
         * 设置增量 判断是否越界 溢出 如果大于len 返回0 否则返回 i + 1
         */
        //获取环形数组的下一个索引
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * Decrement i modulo len.
         * 设置减量 判断是否越界 溢出 如果i - 1 > 0 返回i - 1 否则返回 len - 1
         */
        //获取环形数组的上一个索引
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it.
         *
         * 可以看出来线程中的ThreadLocalMap使用的是延迟初始化，在第一次调用get()或者set()方法的
         */

        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            //初始化table
            table = new Entry[INITIAL_CAPACITY];
            //计算索引
            //关于& (INITIAL_CAPACITY - 1),这是取模的一种方式，
            // 对于2的幂作为模数取模，用此代替 %(2^n)，这也就是为啥容量必须为2的冥，

            //定义了一个AtomicInteger类型，每次获取当前值
            // 并加上HASH_INCREMENT，HASH_INCREMENT = 0x61c88647,
            // 关于这个值和斐波那契散列有关，其原理这里不再深究，感兴趣可自行搜索，
            // 其主要目的就是为了让哈希码能均匀的分布在2的n次方的数组里, 也就是Entry[] table中。

            //可以说在ThreadLocalMap中，
            // 形如key.threadLocalHashCode & (table.length - 1)
            // （其中key为一个ThreadLocal实例）这样的代码片段实质上就是在
            // 求一个ThreadLocal实例的哈希值，只是在源码实现中没有将其抽为一个公用函数。
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            //设置值
            table[i] = new Entry(firstKey, firstValue);
            // 设置节点表大小为1
            size = 1;
            //设置阙值
            setThreshold(INITIAL_CAPACITY);
        }

        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * Get the entry associated with key.  This method
         * itself handles only the fast path: a direct hit of existing
         * key. It otherwise relays to getEntryAfterMiss.  This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */

        private Entry getEntry(ThreadLocal<?> key) {
            //根据key计算索引，获取entry
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            // 对应的entry存在且未失效且弱引用指向的ThreadLocal就是key，则命中返回
            if (e != null && e.get() == key)
                return e;
            else
                // 因为用的是线性探测，所以往后找还是有可能能够找到目标Entry的。
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * Version of getEntry method for use when key is not found in
         * its direct hash slot.
         *
         * @param  key the thread local object
         * @param  i the table index for key's hash code
         * @param  e the entry at table[i]
         * @return the entry associated with key, or null if no such
         */
        /**
         * 通过直接计算出来的key找不到对于的value的时候适用这个方法.
         * 调用getEntry未直接命中的时候调用此方法
         */

        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            Entry[] tab = table;
            int len = tab.length;

            // 基于线性探测法不断向后探测直到遇到空entry。
            while (e != null) {
                ThreadLocal<?> k = e.get();
                if (k == key)
                    return e;
                if (k == null)
                    //清除无效的entry
                    expungeStaleEntry(i);
                else
                    //基于线性探测法向后扫描
                    i = nextIndex(i, len);
                e = tab[i];
            }
            return null;
        }

        /**
         * Set the value associated with key.
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        /**
         ThreadLocalMap使用线性探测法来解决哈希冲突，
         线性探测法的地址增量di = 1, 2, ... , m-1
         其中，i为探测次数。该方法一次探测下一个地址，
         直到有空的地址后插入，若整个空间都找不到空余的地址，
         则产生溢出。假设当前table长度为16，
         也就是说如果计算出来key的hash值为14，如果table[14]上已经有值，
         并且其key与当前key不一致，那么就发生了hash冲突，这个时候将14加1得到15，
         取table[15]进行判断，这个时候如果还是冲突会回到0，取table[0],以此类推，直到可以插入。

         可以把table看成一个 环形数组。
         **/


        private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            Entry[] tab = table;
            int len = tab.length;
            //计算索引，上面已经有说过。
            int i = key.threadLocalHashCode & (len-1);
            /**
             * 根据获取到的索引进行循环，如果当前索引上的table[i]不为空，在没有return的情况下，
             * 就使用nextIndex()获取下一个（上面提到到线性探测法）。
             */
            // 线性探测
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();
                //table[i]上key不为空，并且和当前key相同，更新value
                // 找到对应的entry
                if (k == key) {
                    e.value = value;
                    return;
                }
                /**
                 * table[i]上的key为空，说明被回收了（上面的弱引用中提到过）。
                 * 这个时候说明改table[i]可以重新使用，用新的key-value将其替换,并删除其他无效的entry
                 */
                // 替换失效的entry
                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }
            //找到为空的插入位置，插入值，在为空的位置插入需要对size进行加1操作
            tab[i] = new Entry(key, value);
            int sz = ++size;
            /**
             * cleanSomeSlots用于清除那些e.get()==null，
             * 也就是table[index] != null && table[index].get()==null
             * 之前提到过，这种数据key关联的对象已经被回收，
             * 所以这个Entry(table[index])可以被置null。
             * 如果没有清除任何entry,并且当前使用量达到了负载因子所定义(长度的2/3)，
             * 那么进行rehash()
             */
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * Remove the entry for key.
         */
        //remove()在有上面了解后可以说极为简单了，
        // 就是找到对应的table[],调用weakrefrence的clear()清除引用，
        // 然后再调用expungeStaleEntry()进行清除。
        /**
         * 从ThreadLocalMap中删除ThreadLocal
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            //计算索引
            int i = key.threadLocalHashCode & (len-1);
            //进行线性探测，查找正确的key
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    //调用weakrefrence的clear()清除引用
                    e.clear();
                    //连续段清除
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * Replace a stale entry encountered during a set operation
         * with an entry for the specified key.  The value passed in
         * the value parameter is stored in the entry, whether or not
         * an entry already exists for the specified key.
         *
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         *
         * @param  key the key
         * @param  value the value to be associated with key
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         */
        //替换无效entry
        private void replaceStaleEntry(ThreadLocal<?> key, Object value,
                                       int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
            /**
             * 根据传入的无效entry的位置（staleSlot）,向前扫描
             * 一段连续的entry(这里的连续是指一段相邻的entry并且table[i] != null),
             * 直到找到一个无效entry，或者扫描完也没找到
             */
            // 向前扫描，查找最前的一个无效slot
            //获取当前索引  之后用于清理的起点
            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len);//前一个索引
                 (e = tab[i]) != null;//前一个索引如果 不为null
                 i = prevIndex(i, len))//继续往前找
                //前一个索引的 key为 null
                if (e.get() == null)
                    //将前一个索引 替换为当前索引
                    slotToExpunge = i;

            // Find either the key or trailing null slot of run, whichever
            // occurs first
            /**
             * 向后扫描一段连续的entry
             */
            // 向后遍历table
            for (int i = nextIndex(staleSlot, len); //当前索引的下一个索引
                 (e = tab[i]) != null; //下一个索引的位置不为 null
                 i = nextIndex(i, len)) {//继续下一个索引

                ThreadLocal<?> k = e.get();//获得下一个索引的的 key

                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                //下一个索引和 当前索引的key相同 将当前索引的值赋值给下一个索引
                /**
                 * 如果找到了key，将其与传入的无效entry替换，也就是与table[staleSlot]进行替换
                 */
                // 找到了key，将其与无效的slot交换
                if (k == key) {
                    // 更新对应slot的value值
                    e.value = value;
                    //当前的位置 赋值给下一个位置
                    tab[i] = tab[staleSlot];
                    //当前位置索引变为下一个位置
                    tab[staleSlot] = e;

                    // Start expunge at preceding stale entry if it exists
                    //如果向前查找没有找到无效entry，则更新slotToExpunge为当前值i
                    /*
                     * 如果在整个扫描过程中（包括函数一开始的向前扫描与i之前的向后扫描）
                     * 找到了之前的无效slot则以那个位置作为清理的起点，
                     * 否则则以当前的i作为清理起点
                     */
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;
                    // 从slotToExpunge开始做一次连续段的清理，再做一次启发式清理
                    cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
                    return;
                }

                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                /**
                 * 如果向前查找没有找到无效entry，并且当前向后扫描的entry无效，
                 * 则更新slotToExpunge为当前值i
                 */
                // 如果当前的slot已经无效，并且向前扫描过程中没有无效slot，则更新slotToExpunge为当前位置
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // If key not found, put new entry in stale slot

            /**
             * 如果没有找到key,也就是说key之前不存在table中
             * 就直接最开始的无效entry——tab[staleSlot]上直接新增即可
             */
            // 如果key在table中不存在，则在原地放一个即可
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            // If there are any other stale entries in run, expunge them
            /**
             * slotToExpunge != staleSlot,说明存在其他的无效entry需要进行清理。
             */
            // 在探测过程中如果发现任何无效slot，则做一次清理（连续段清理+启发式清理）
            if (slotToExpunge != staleSlot)
                cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);
        }

        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null.  See
         * Knuth, Section 6.4
         *
         * @param staleSlot index of slot known to have null key
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).
         */
        /**
         * 连续段清除
         * 根据传入的staleSlot,清理对应的无效entry——table[staleSlot],
         * 并且根据当前传入的staleSlot,向后扫描一段连续的entry
         * (这里的连续是指一段相邻的entry并且table[i] != null),
         * 对可能存在hash冲突的entry进行rehash，并且清理遇到的无效entry.
         *
         * @param staleSlot key为null,需要无效entry所在的table中的索引
         * @return 返回下一个为空的solt的索引。
         */

        /**
         * 这个函数是ThreadLocal中核心清理函数，它做的事情很简单：
         * 就是从staleSlot开始遍历，将无效（弱引用指向对象被回收）清理，
         * 即对应entry中的value置为null，将指向这个entry的table[i]置为null，直到扫到空entry。
         * 另外，在过程中还会对非空的entry作rehash。
         * 可以说这个函数的作用就是从staleSlot开始清理连续段中的slot（断开强引用，rehash slot等）
         */
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // expunge entry at staleSlot
            // 清理无效entry，置空
            // 因为entry对应的ThreadLocal已经被回收，value设为null，显式断开强引用
            tab[staleSlot].value = null;
            // 显式设置该entry为null，以便垃圾回收
            tab[staleSlot] = null;
            //size减1，置空后table的被使用量减1
            size--;

            // Rehash until we encounter null
            Entry e;
            int i;
            /**
             * 从staleSlot开始向后扫描一段连续的entry
             */
            for (i = nextIndex(staleSlot, len);
                 (e = tab[i]) != null;
                 i = nextIndex(i, len)) {
                ThreadLocal<?> k = e.get();
                //如果遇到key为null,表示无效entry，进行清理.
                // 清理对应ThreadLocal已经被回收的entry
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                } else {
                    /*
                     * 对于还没有被回收的情况，需要做一次rehash。
                     *
                     * 如果对应的ThreadLocal的ID对len取模出来的索引h不为当前位置i，
                     * 则从h向后线性探测到第一个空的slot，把当前的entry给挪过去。
                     */
                    //如果key不为null,计算索引
                    int h = k.threadLocalHashCode & (len - 1);
                    /**
                     * 计算出来的索引——h，与其现在所在位置的索引——i不一致，置空当前的table[i]
                     * 从h开始向后线性探测到第一个空的slot，把当前的entry挪过去。
                     */
                    if (h != i) {
                        tab[i] = null;
                        /*
                         * 在原代码的这里有句注释值得一提，原注释如下：
                         *
                         * Unlike Knuth 6.4 Algorithm R, we must scan until
                         * null because multiple entries could have been stale.
                         *
                         * 这段话提及了Knuth高德纳的著作TAOCP（《计算机程序设计艺术》）的6.4章节（散列）
                         * 中的R算法。R算法描述了如何从使用线性探测的散列表中删除一个元素。
                         * R算法维护了一个上次删除元素的index，
                         * 当在非空连续段中扫到某个entry的哈希值取模后的索引
                         * 还没有遍历到时，会将该entry挪到index那个位置，并更新当前位置为新的index，
                         * 继续向后扫描直到遇到空的entry。
                         *
                         * ThreadLocalMap因为使用了弱引用，所以其实每个slot的状态有三种也即
                         * 有效（value未回收），无效（value已回收），空（entry==null）。
                         * 正是因为ThreadLocalMap的entry有三种状态，所以不能完全套高德纳原书的R算法。
                         *
                         * 因为expungeStaleEntry函数在扫描过程中还会对无效slot清理将之转为空slot，
                         * 如果直接套用R算法，可能会出现具有相同哈希值的entry之间断开（中间有空entry）。
                         */

                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        //从h开始向后线性探测到第一个空的slot，把当前的entry挪过去。
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }
            //下一个为空的solt的索引。
            // 返回staleSlot之后第一个空的slot索引
            return i;
        }

        /**
         * Heuristically scan some cells looking for stale entries.
         * This is invoked when either a new element is added, or
         * another stale one has been expunged. It performs a
         * logarithmic number of scans, as a balance between no
         * scanning (fast but retains garbage) and a number of scans
         * proportional to number of elements, that would find all
         * garbage but would cause some insertions to take O(n) time.
         *
         * @param i a position known NOT to hold a stale entry. The
         * scan starts at the element after i.
         *
         * @param n scan control: {@code log2(n)} cells are scanned,
         * unless a stale entry is found, in which case
         * {@code log2(table.length)-1} additional cells are scanned.
         * When called from insertions, this parameter is the number
         * of elements, but when from replaceStaleEntry, it is the
         * table length. (Note: all this could be changed to be either
         * more or less aggressive by weighting n instead of just
         * using straight log n. But this version is simple, fast, and
         * seems to work well.)
         *
         * @return true if any stale entries have been removed.
         */
        /**
         * cleanSomeSlots用于清除那些e.get()==null，
         * 也就是table[index] != null && table[index].get()==null
         * 之前提到过，这种数据key关联的对象已经被回收，所以这个Entry(table[index])可以被置null。
         */
        /**
         * 启发式的扫描清除，扫描次数由传入的参数n决定
         *
         * @param i 从i向后开始扫描（不包括i，因为索引为i的Slot肯定为null）
         *
         * @param n 控制扫描次数，正常情况下为 log2(n) ，
         * 如果找到了无效entry，会将n重置为table的长度len,进行段清除。
         *
         * map.set()点用的时候传入的是元素个数，replaceStaleEntry()调用的时候传入的是table的长度len
         *
         * @return true if any stale entries have been removed.
         */
        /**
         * 启发式地清理slot,
         * i对应entry是非无效（指向的ThreadLocal没被回收，或者entry本身为空）
         * n是用于控制控制扫描次数的
         * 正常情况下如果log n次扫描没有发现无效slot，函数就结束了
         * 但是如果发现了无效的slot，将n置为table的长度len，做一次连续段的清理
         * 再从下一个空的slot开始继续扫描
         *
         * 这个函数有两处地方会被调用，一处是插入的时候可能会被调用，另外个是在替换无效slot的时候可能会被调用，
         * 区别是前者传入的n为元素个数，后者为table的容量
         */

        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                // i在任何情况下自己都不会是一个无效slot，所以从下一个开始判断
                i = nextIndex(i, len); //获取下一个位置
                //获取对应的entry
                Entry e = tab[i];
                //entry不为null 但是 key为null 代表被回收 删除
                if (e != null && e.get() == null) {
                    //重置n为len
                    // 扩大扫描控制因子
                    n = len;
                    removed = true;
                    //依然调用expungeStaleEntry来进行无效entry的清除 // 清理一个连续段
                    i = expungeStaleEntry(i);
                }
            } while ( (n >>>= 1) != 0);//无符号的右移动，可以用于控制扫描次数在log2(n)
            return removed;
        }

        /**
         * Re-pack and/or re-size the table. First scan the entire
         * table removing stale entries. If this doesn't sufficiently
         * shrink the size of the table, double the table size.
         */
        //重建 table数组 删除旧的entry
        private void rehash() {
            //全清理
            // 做一次全量清理
            expungeStaleEntries();

            // Use lower threshold for doubling to avoid hysteresis
            /**
             * threshold = 2/3 * len
             * 所以threshold - threshold / 4 = 1en/2
             * 这里主要是因为上面做了一次全清理所以size减小，需要进行判断。
             * 判断的时候把阈值调低了。
             */

            /*
             * 因为做了一次清理，所以size很可能会变小。
             * ThreadLocalMap这里的实现是调低阈值来判断是否需要扩容，
             * threshold默认为len*2/3，所以这里的threshold - threshold / 4相当于len/2
             */
            //size 大于等于 1/2 的len 扩容
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * Double the capacity of the table.
         */
        /**
         * 扩容，扩大为原来的2倍（这样保证了长度为2的冥）
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            //扩容为 原来的 2倍
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    //虽然做过一次清理，但在扩容的时候可能会又存在key==null的情况。
                    if (k == null) {
                        //这里试试将e.value设置为null
                        e.value = null; // Help the GC
                    } else {
                        //同样适用线性探测来设置值
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }
            //设置新的阈值
            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * Expunge all stale entries in the table.
         */

        /**
         * 全清理，清理所有无效entry
         */
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    //使用连续段清理
                    /*
                     * 个人觉得这里可以取返回值，如果大于j的话取了用，这样也是可行的。
                     * 因为expungeStaleEntry执行过程中是把连续段内所有无效slot都清理了一遍了。
                     */
                    expungeStaleEntry(j);
            }
        }
    }
}
