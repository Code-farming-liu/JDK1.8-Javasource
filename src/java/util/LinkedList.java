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

package java.util;

import java.util.function.Consumer;

/**
 * Doubly-linked list implementation of the {@code List} and {@code Deque}
 * interfaces.  Implements all optional list operations, and permits all
 * elements (including {@code null}).
 *
 * <p>All of the operations perform as could be expected for a doubly-linked
 * list.  Operations that index into the list will traverse the list from
 * the beginning or the end, whichever is closer to the specified index.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a linked list concurrently, and at least
 * one of the threads modifies the list structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more elements; merely setting the value of
 * an element is not a structural modification.)  This is typically
 * accomplished by synchronizing on some object that naturally
 * encapsulates the list.
 * <p>
 * If no such object exists, the list should be "wrapped" using the
 * {@link Collections#synchronizedList Collections.synchronizedList}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the list:<pre>
 *   List list = Collections.synchronizedList(new LinkedList(...));</pre>
 *
 * <p>The iterators returned by this class's {@code iterator} and
 * {@code listIterator} methods are <i>fail-fast</i>: if the list is
 * structurally modified at any time after the iterator is created, in
 * any way except through the Iterator's own {@code remove} or
 * {@code add} methods, the iterator will throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than
 * risking arbitrary, non-deterministic behavior at an undetermined
 * time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:   <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <E> the type of elements held in this collection
 * @author Josh Bloch
 * @see List
 * @see ArrayList
 * @since 1.2
 */

public class LinkedList<E>
        extends AbstractSequentialList<E>
        implements List<E>, Deque<E>, Cloneable, java.io.Serializable {
    transient int size = 0;

    /**
     * 指向第一个节点的指针。
     * Invariant: (first == null && last == null) ||
     * (first.prev == null && first.item != null)
     */
    transient Node<E> first;

    /**
     * 指向最后一个节点的指针。
     * Invariant: (first == null && last == null) ||
     * (last.next == null && last.item != null)
     */
    transient Node<E> last;

    /**
     * 构造一个空列表。
     */
    public LinkedList() {
    }

    /**
     * 构造一个包含指定元素的列表
     * 集合，按集合的返回顺序
     * 迭代器。
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    public LinkedList(Collection<? extends E> c) {
        this();
        //将元素添加到集合中
        addAll(c);
    }

    /**
     * 将e链接为第一个元素。
     */
    private void linkFirst(E e) {
        //保存原节点的头结点
        final Node<E> f = first;
        //将原节点的 头结点 作为 新节点的后置节点
        final Node<E> newNode = new Node<>(null, e, f);
        //更新 first 头指针为 头结点
        first = newNode;
        //空链表 也就是 直接将 尾指针 指向头结点
        if (f == null)
            last = newNode;
        else
            //否则将原节点的前驱 设置为 新节点 也就是插入到原节点之前
            f.prev = newNode;
        //修改数量
        size++;
        //修改 modCount
        modCount++;
    }

    /**
     * 将e链接为最后一个元素。
     */
    //生成新节点 并插入到 链表尾部， 更新 last/first 节点。
    void linkLast(E e) {
        //记录原尾部节点
        final Node<E> l = last;
        //以原尾部节点为新节点的前置节点 后继节点 为 null
        final Node<E> newNode = new Node<>(l, e, null);
        //更新尾部节点
        last = newNode;
        //若原链表为空链表，需要额外更新头结点
        if (l == null)
            first = newNode;
        else
            //否则更新原尾节点的后置节点为现在的尾节点（新节点）
            l.next = newNode;
        //修改size
        size++;
        //修改modCount
        modCount++;
    }

    /**
     * 在非null节点succ之前插入元素e。
     */
    //在succ节点前，插入一个新节点e
    void linkBefore(E e, Node<E> succ) {
        // assert succ != null;
        //保存后置节点的前置节点
        final Node<E> pred = succ.prev;
        //以前置和后置节点和元素值e 构建一个新节点
        final Node<E> newNode = new Node<>(pred, e, succ);
        //新节点newNode是原节点succ的前置节点
        succ.prev = newNode;
        //如果之前的前置节点是空，说明succ是原头结点。所以新节点是现在的头结点
        if (pred == null)
            first = newNode;
        else
            //否则构建原节点的前置节点的后置节点为new
            pred.next = newNode;
        //修改数量
        size++;
        //修改 modCount
        modCount++;
    }

    /**
     * 取消链接非空的第一个节点f.
     */
    private E unlinkFirst(Node<E> f) {
        // assert f == first && f != null;
        //节点的元素
        final E element = f.item;
        //节点的后置节点
        final Node<E> next = f.next;
        //元素 赋空
        f.item = null;
        //后置为null
        f.next = null; // help GC
        //头指针 指向 节点的下一个节点
        first = next;
        //下一个节点为null 也就是该节点是头结点
        if (next == null)
            //尾指针为null
            last = null;
        else
            //下一个节点的前置节点为空
            next.prev = null;
        //改变数量
        size--;
        //修改modCount
        modCount++;
        //返回 取出的元素
        return element;
    }

    /**
     * 取消链接非空的最后一个节点l。
     */
    private E unlinkLast(Node<E> l) {
        // assert l == last && l != null;
        //保存原来的 元素
        final E element = l.item;
        //原来节点的 前置节点
        final Node<E> prev = l.prev;
        //原来节点的 元素为null
        l.item = null;
        //前驱 为空
        l.prev = null; // help GC
        //尾指针为 前一个节点 尾指针 前移
        last = prev;
        // 前一个节点为 null 证明他是头结点
        if (prev == null)
            //头指针 赋值为null
            first = null;
        else
            // 将前置节点的 后继 指向null
            prev.next = null;
        //改变数量
        size--;
        //修改modCount
        modCount++;
        //返回 取出的元素
        return element;
    }

    /**
     * 取消链接非空节点x。
     */
    //从链表上删除x节点
    E unlink(Node<E> x) {
        // assert x != null;
        //当前节点的元素值
        final E element = x.item;
        //当前节点的后置节点
        final Node<E> next = x.next;
        //当前节点的前置节点
        final Node<E> prev = x.prev;

        //如果前置节点为空(说明当前节点原本是头结点)
        if (prev == null) {
            //则头结点等于后置节点  直接删除
            first = next;
        } else {
            //也就是 将当前节点的 前一个节点的 后继 指向 当前节点的后一个节点
            // 1 --- 2 --- 3  ====== 1 ---- 3
            prev.next = next;
            //将当前节点的 前置节点置空
            x.prev = null;
        }
        //如果后置节点为空（说明当前节点原本是尾节点）
        if (next == null) {
            //直接将尾指针 前移 指向 前置节点 //则 尾节点为前置节点
            last = prev;
        } else {
            //否则 不是 尾节点  也就是 将当前节点的 后一个节点的 前驱 指向 当前节点的前一个节点
            next.prev = prev;
            //将当前节点的 后置节点置空
            x.next = null;
        }
        //将当前元素值置空
        x.item = null;
        //修改数量
        size--;
        //修改modCount
        modCount++;
        //返回取出的元素值
        return element;
    }

    /**
     * 返回此列表中的第一个元素。
     *
     * @return the first element in this list
     * @throws NoSuchElementException if this list is empty
     */
    public E getFirst() {
        //保存原来的头指针
        final Node<E> f = first;
        //原链表 为空 抛出异常 否则返回第一个元素
        if (f == null)
            throw new NoSuchElementException();
        return f.item;
    }

    /**
     * Returns the last element in this list.
     *
     * @return the last element in this list
     * @throws NoSuchElementException if this list is empty
     */
    public E getLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return l.item;
    }

    /**
     * 从此列表中删除并返回第一个元素。
     *
     * @return the first element from this list
     * @throws NoSuchElementException if this list is empty
     */
    public E removeFirst() {
        final Node<E> f = first;
        // 为空 抛出异常 否则返回值
        if (f == null)
            throw new NoSuchElementException();
        return unlinkFirst(f);
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return the last element from this list
     * @throws NoSuchElementException if this list is empty
     */
    public E removeLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return unlinkLast(l);
    }

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * @param e the element to add
     */
    public void addFirst(E e) {
        linkFirst(e);
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @param e the element to add
     */
    public void addLast(E e) {
        linkLast(e);
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element
     */
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    public int size() {
        return size;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * <p>This method is equivalent to {@link #addLast}.
     *
     * @param e element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    //在尾部插入一个节点： add
    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If this list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    //因为要考虑 null元素，也是分情况遍历 底层都是 unlink方法
    public boolean remove(Object o) {
        //如果要删除的是null节点(从remove和add 里 可以看出，允许元素为null)
        if (o == null) {
            //遍历每个节点 对比
            for (Node<E> x = first; x != null; x = x.next) {
                //节点的值为 null
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            //要删除的不是null
            //遍历 所有节点 寻找一个和元素 相同的 删除
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's iterator.  The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in
     * progress.  (Note that this will occur if the specified collection is
     * this list, and it's nonempty.)
     *
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    //在尾部开始添加元素
    public boolean addAll(Collection<? extends E> c) {
        //从链表 尾 开始添加元素
        return addAll(size, c);
    }

    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param index index at which to insert the first element
     *              from the specified collection
     * @param c     collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException      if the specified collection is null
     */
    //以index为插入下标，插入集合c中所有元素
    public boolean addAll(int index, Collection<? extends E> c) {
        // 1.添加位置的下标的合理性检查
        checkPositionIndex(index);
        // 2.将集合转换为Object[]数组对象
        Object[] a = c.toArray();
        //新增的 元素数量
        int numNew = a.length;
        //如果新增元素数量为0，则不增加，并返回false
        if (numNew == 0)
            return false;
        // 3.得到插入位置的前驱节点和后继节点
        //pred  前驱
        //succ 后继
        Node<E> pred, succ;
        //尾部插入
        if (index == size) {
            //新节点 后继为空
            succ = null;
            //将新节点的 前驱指针 指向 原有最后一个节点
            pred = last;
        } else {
            // 从指定位置（非尾部）添加的情况:
            //后继为index
            succ = node(index);
            //前驱为 index的 前一个
            pred = succ.prev;
        }
        //链表批量增加，是靠for循环遍历原数组，依次执行插入节点操作。
        // 对比ArrayList是通过System.arraycopy完成批量增加的
        for (Object o : a) {
            @SuppressWarnings("unchecked") E e = (E) o;
            Node<E> newNode = new Node<>(pred, e, null);
            //第一个节点 头节点
            if (pred == null)
                first = newNode;
            else
                //否则 前置节点的后置节点设置问新节点
                pred.next = newNode;
            //相当于 前进一步，当前的节点为前置节点了，为下次添加节点做准备
            pred = newNode;
        }

        //最后一个节点 也就是 尾节点 链表尾添加
        if (succ == null) {
            //则设置尾节点 新节点的前驱 设置为 尾节点
            last = pred;
        } else {
            // 否则是在队中插入的节点 ，更新前置节点 后置节点
            pred.next = succ;
            //更新后置节点的前置节点
            succ.prev = pred;
        }
        // 修改数量size
        size += numNew;
        //修改modCount
        modCount++;
        return true;
    }

    /**
     * Removes all of the elements from this list.
     * The list will be empty after this call returns.
     */
    //遍历将所有节点 置空
    public void clear() {
        // Clearing all of the links between nodes is "unnecessary", but:
        // - helps a generational GC if the discarded nodes inhabit
        //   more than one generation
        // - is sure to free memory even if there is a reachable Iterator
        for (Node<E> x = first; x != null; ) {
            Node<E> next = x.next;
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }
        first = last = null;
        size = 0;
        modCount++;
    }


    // Positional Access Operations

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    //获得index 位置的 元素值
    public E get(int index) {
        //判断是否越界 [0,size)
        checkElementIndex(index);
        //调用node()方法 取出 Node节点，
        return node(index).item;
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *
     * @param index   index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    //将该index位置的值 设置为 element
    public E set(int index, E element) {
        //检查越界[0,size)
        checkElementIndex(index);
        //取出对应的Node
        Node<E> x = node(index);
        //保存旧值 供返回
        E oldVal = x.item;
        //用新值覆盖旧值
        x.item = element;
        //返回旧值
        return oldVal;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any
     * subsequent elements to the right (adds one to their indices).
     *
     * @param index   index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    //在指定下标，index处，插入一个节点
    public void add(int index, E element) {
        //检查下标是否越界[0,size]
        checkPositionIndex(index);
        //在尾节点后插入
        if (index == size)
            linkLast(element);
        else
            //在中间插入
            linkBefore(element, node(index));
    }

    /**
     * Removes the element at the specified position in this list.  Shifts any
     * subsequent elements to the left (subtracts one from their indices).
     * Returns the element that was removed from the list.
     *
     * @param index the index of the element to be removed
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    //删：remove目标节点
    public E remove(int index) {
        //检查是否越界 下标[0,size)
        checkElementIndex(index);
        //从链表上删除某节点
        return unlink(node(index));
    }

    /**
     * 判断参数是否为现有元素的索引。
     */
    private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }

    /**
     * 判断参数是否为有效头寸的索引
     * 迭代器或添加操作。
     */
    private boolean isPositionIndex(int index) {
        return index >= 0 && index <= size;
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     */
    private String outOfBoundsMsg(int index) {
        return "Index: " + index + ", Size: " + size;
    }

    private void checkElementIndex(int index) {
        if (!isElementIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private void checkPositionIndex(int index) {
        if (!isPositionIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * 返回指定元素索引处的（非空）节点。
     */

    Node<E> node(int index) {
        // assert isElementIndex(index);
        //通过下标获取某个node 的时候，（增、查 ），会根据index处于前半段还是后半段 进行一个折半，
        // 以提升查询效率
        //前半段
        if (index < (size >> 1)) {
            //遍历指针
            Node<E> x = first;
            //遍历查找 找到 返回该节点
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
            //后半段
            Node<E> x = last;
            //从后往前找
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }

    // Search Operations

    /**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the lowest index {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *
     * @param o element to search for
     * @return the index of the first occurrence of the specified element in
     * this list, or -1 if this list does not contain the element
     */
    //根据节点对象，查询下标 从前往后遍历 否则 返回 -1
    public int indexOf(Object o) {
        int index = 0;
        //如果目标对象是null
        if (o == null) {
            //遍历 然后返回第一个 为 null的 下标
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null)
                    return index;
                index++;
            }
        } else {
            //对象不为null 返回与查找对象相等的元素的 下标
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item))
                    return index;
                index++;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the last occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the highest index {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *
     * @param o element to search for
     * @return the index of the last occurrence of the specified element in
     * this list, or -1 if this list does not contain the element
     */
    //从尾至头遍历链表，找到目标元素值为o的节点 从后往前遍历 否则返回 -1
    public int lastIndexOf(Object o) {
        int index = size;
        //如果目标对象是null
        if (o == null) {
            //遍历 然后返回第一个 为 null的 下标
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (x.item == null)
                    return index;
            }
        } else {
            //对象不为null 返回与查找对象相等的元素的 下标
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (o.equals(x.item))
                    return index;
            }
        }
        return -1;
    }

    // Queue operations.

    /**
     * 检索但不删除此列表的头（第一个元素）。
     *
     * @return the head of this list, or {@code null} if this list is empty
     * @since 1.5
     */
    //查看链表的第一个元素
    public E peek() {
        //保存原来链表的 头指针
        final Node<E> f = first;
        //如果原来是空链表 返回null 否则 返回 头节点的元素
        return (f == null) ? null : f.item;
    }

    /**
     * 检索但不删除此列表的头（第一个元素）。
     *
     * @return the head of this list
     * @throws NoSuchElementException if this list is empty
     * @since 1.5
     */

    public E element() {
        return getFirst();
    }

    /**
     * 检索并删除此列表的头（第一个元素）。
     *
     * @return the head of this list, or {@code null} if this list is empty
     * @since 1.5
     */
    public E poll() {
        final Node<E> f = first;
        //如果 为空返回 null 否则返回取出的元素
        return (f == null) ? null : unlinkFirst(f);
    }

    /**
     * 检索并删除此列表的头（第一个元素）。
     *
     * @return the head of this list
     * @throws NoSuchElementException if this list is empty
     * @since 1.5
     */

    public E remove() {
        return removeFirst();
    }

    /**
     * 将指定的元素添加为此列表的尾部（最后一个元素）。
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Queue#offer})
     * @since 1.5
     */
    public boolean offer(E e) {
        return add(e);
    }

    // Deque operations

    /**
     * 将指定的元素插入此列表的前面。
     *
     * @param e the element to insert
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     * @since 1.6
     */
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    /**
     * 将指定的元素插入此列表的末尾。
     *
     * @param e the element to insert
     * @return {@code true} (as specified by {@link Deque#offerLast})
     * @since 1.6
     */
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    /**
     * 检索但不删除此列表的第一个元素，
     * or returns {@code null} if this list is empty.
     *
     * @return the first element of this list, or {@code null}
     * if this list is empty
     * @since 1.6
     */
    public E peekFirst() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }

    /**
     * 检索但不删除此列表的最后一个元素，
     * or returns {@code null} if this list is empty.
     *
     * @return the last element of this list, or {@code null}
     * if this list is empty
     * @since 1.6
     */
    public E peekLast() {
        final Node<E> l = last;
        return (l == null) ? null : l.item;
    }

    /**
     * 检索并删除此列表的第一个元素，
     * or returns {@code null} if this list is empty.
     *
     * @return the first element of this list, or {@code null} if
     * this list is empty
     * @since 1.6
     */
    public E pollFirst() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    /**
     * 检索并删除此列表的最后一个元素，
     * or returns {@code null} if this list is empty.
     *
     * @return the last element of this list, or {@code null} if
     * this list is empty
     * @since 1.6
     */
    public E pollLast() {
        final Node<E> l = last;
        return (l == null) ? null : unlinkLast(l);
    }

    /**
     * 将元素压入此列表表示的堆栈。换句话说
     * ，将元素插入此列表的前面。
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *
     * @param e the element to push
     * @since 1.6
     */
    public void push(E e) {
        addFirst(e);
    }

    /**
     * 从此列表表示的堆栈中弹出一个元素。换句话说，删除并返回此列表的第一个元素。
     *
     * <p>This method is equivalent to {@link #removeFirst()}.
     *
     * @return the element at the front of this list (which is the top
     * of the stack represented by this list)
     * @throws NoSuchElementException if this list is empty
     * @since 1.6
     */
    public E pop() {
        return removeFirst();
    }

    /**
     * Removes the first occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     * @since 1.6
     */
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    /**
     * Removes the last occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     * @since 1.6
     */
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a list-iterator of the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * Obeys the general contract of {@code List.listIterator(int)}.<p>
     * <p>
     * The list-iterator is <i>fail-fast</i>: if the list is structurally
     * modified at any time after the Iterator is created, in any way except
     * through the list-iterator's own {@code remove} or {@code add}
     * methods, the list-iterator will throw a
     * {@code ConcurrentModificationException}.  Thus, in the face of
     * concurrent modification, the iterator fails quickly and cleanly, rather
     * than risking arbitrary, non-deterministic behavior at an undetermined
     * time in the future.
     *
     * @param index index of the first element to be returned from the
     *              list-iterator (by a call to {@code next})
     * @return a ListIterator of the elements in this list (in proper
     * sequence), starting at the specified position in the list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @see List#listIterator(int)
     */
    public ListIterator<E> listIterator(int index) {
        checkPositionIndex(index);
        return new ListItr(index);
    }

    private class ListItr implements ListIterator<E> {
        private Node<E> lastReturned;
        private Node<E> next;
        private int nextIndex;
        private int expectedModCount = modCount;

        ListItr(int index) {
            // assert isPositionIndex(index);
            next = (index == size) ? null : node(index);
            nextIndex = index;
        }

        public boolean hasNext() {
            return nextIndex < size;
        }

        public E next() {
            checkForComodification();
            if (!hasNext())
                throw new NoSuchElementException();

            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.item;
        }

        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        public E previous() {
            checkForComodification();
            if (!hasPrevious())
                throw new NoSuchElementException();

            lastReturned = next = (next == null) ? last : next.prev;
            nextIndex--;
            return lastReturned.item;
        }

        public int nextIndex() {
            return nextIndex;
        }

        public int previousIndex() {
            return nextIndex - 1;
        }

        public void remove() {
            checkForComodification();
            if (lastReturned == null)
                throw new IllegalStateException();

            Node<E> lastNext = lastReturned.next;
            unlink(lastReturned);
            if (next == lastReturned)
                next = lastNext;
            else
                nextIndex--;
            lastReturned = null;
            expectedModCount++;
        }

        public void set(E e) {
            if (lastReturned == null)
                throw new IllegalStateException();
            checkForComodification();
            lastReturned.item = e;
        }

        public void add(E e) {
            checkForComodification();
            lastReturned = null;
            if (next == null)
                linkLast(e);
            else
                linkBefore(e, next);
            nextIndex++;
            expectedModCount++;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            while (modCount == expectedModCount && nextIndex < size) {
                action.accept(next.item);
                lastReturned = next;
                next = next.next;
                nextIndex++;
            }
            checkForComodification();
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    //基本存储单元 双向链表
    private static class Node<E> {
        E item; //元素
        Node<E> next;// 后继节点
        Node<E> prev;// 前驱节点

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    /**
     * @since 1.6
     */
    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    /**
     * Adapter to provide descending iterators via ListItr.previous
     */
    private class DescendingIterator implements Iterator<E> {
        private final ListItr itr = new ListItr(size());

        public boolean hasNext() {
            return itr.hasPrevious();
        }

        public E next() {
            return itr.previous();
        }

        public void remove() {
            itr.remove();
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedList<E> superClone() {
        try {
            return (LinkedList<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Returns a shallow copy of this {@code LinkedList}. (The elements
     * themselves are not cloned.)
     *
     * @return a shallow copy of this {@code LinkedList} instance
     */
    public Object clone() {
        LinkedList<E> clone = superClone();

        // Put clone into "virgin" state
        clone.first = clone.last = null;
        clone.size = 0;
        clone.modCount = 0;

        // Initialize clone with our elements
        for (Node<E> x = first; x != null; x = x.next)
            clone.add(x.item);

        return clone;
    }

    /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this list
     * in proper sequence
     */
    public Object[] toArray() {
        //new 一个新数组 然后遍历链表，将每个元素存在数组里，返回
        Object[] result = new Object[size];
        int i = 0;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;
        return result;
    }

    /**
     * Returns an array containing all of the elements in this list in
     * proper sequence (from first to last element); the runtime type of
     * the returned array is that of the specified array.  If the list fits
     * in the specified array, it is returned therein.  Otherwise, a new
     * array is allocated with the runtime type of the specified array and
     * the size of this list.
     *
     * <p>If the list fits in the specified array with room to spare (i.e.,
     * the array has more elements than the list), the element in the array
     * immediately following the end of the list is set to {@code null}.
     * (This is useful in determining the length of the list <i>only</i> if
     * the caller knows that the list does not contain any null elements.)
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a list known to contain only strings.
     * The following code can be used to dump the list into a newly
     * allocated array of {@code String}:
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     * <p>
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the list
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this list
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            a = (T[]) java.lang.reflect.Array.newInstance(
                    a.getClass().getComponentType(), size);
        int i = 0;
        Object[] result = a;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;

        if (a.length > size)
            a[size] = null;

        return a;
    }

    private static final long serialVersionUID = 876323262645176354L;

    /**
     * Saves the state of this {@code LinkedList} instance to a stream
     * (that is, serializes it).
     *
     * @serialData The size of the list (the number of elements it
     * contains) is emitted (int), followed by all of its
     * elements (each an Object) in the proper order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Write out any hidden serialization magic
        s.defaultWriteObject();

        // Write out size
        s.writeInt(size);

        // Write out all elements in the proper order.
        for (Node<E> x = first; x != null; x = x.next)
            s.writeObject(x.item);
    }

    /**
     * Reconstitutes this {@code LinkedList} instance from a stream
     * (that is, deserializes it).
     */
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Read in any hidden serialization magic
        s.defaultReadObject();

        // Read in size
        int size = s.readInt();

        // Read in all elements in the proper order.
        for (int i = 0; i < size; i++)
            linkLast((E) s.readObject());
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#ORDERED}.  Overriding implementations should document
     * the reporting of additional characteristic values.
     *
     * @return a {@code Spliterator} over the elements in this list
     * @implNote The {@code Spliterator} additionally reports {@link Spliterator#SUBSIZED}
     * and implements {@code trySplit} to permit limited parallelism..
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return new LLSpliterator<E>(this, -1, 0);
    }

    /**
     * A customized variant of Spliterators.IteratorSpliterator
     */
    static final class LLSpliterator<E> implements Spliterator<E> {
        static final int BATCH_UNIT = 1 << 10;  // batch array size increment
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final LinkedList<E> list; // null OK unless traversed
        Node<E> current;      // current node; null until initialized
        int est;              // size estimate; -1 until first needed
        int expectedModCount; // initialized when est set
        int batch;            // batch size for splits

        LLSpliterator(LinkedList<E> list, int est, int expectedModCount) {
            this.list = list;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getEst() {
            int s; // force initialization
            final LinkedList<E> lst;
            if ((s = est) < 0) {
                if ((lst = list) == null)
                    s = est = 0;
                else {
                    expectedModCount = lst.modCount;
                    current = lst.first;
                    s = est = lst.size;
                }
            }
            return s;
        }

        public long estimateSize() {
            return (long) getEst();
        }

        public Spliterator<E> trySplit() {
            Node<E> p;
            int s = getEst();
            if (s > 1 && (p = current) != null) {
                int n = batch + BATCH_UNIT;
                if (n > s)
                    n = s;
                if (n > MAX_BATCH)
                    n = MAX_BATCH;
                Object[] a = new Object[n];
                int j = 0;
                do {
                    a[j++] = p.item;
                } while ((p = p.next) != null && j < n);
                current = p;
                batch = j;
                est = s - j;
                return Spliterators.spliterator(a, 0, j, Spliterator.ORDERED);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p;
            int n;
            if (action == null) throw new NullPointerException();
            if ((n = getEst()) > 0 && (p = current) != null) {
                current = null;
                est = 0;
                do {
                    E e = p.item;
                    p = p.next;
                    action.accept(e);
                } while (p != null && --n > 0);
            }
            if (list.modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            if (getEst() > 0 && (p = current) != null) {
                --est;
                E e = p.item;
                current = p.next;
                action.accept(e);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

}
