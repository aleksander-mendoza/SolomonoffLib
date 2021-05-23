use std::alloc::{Layout, Global, handle_alloc_error, Allocator};
use core::mem;
use std::ptr::NonNull;

pub unsafe fn allocate<A>(len: usize) -> *mut A {
    let ptr = Global.allocate(Layout::array::<A>(len).unwrap());
    if ptr.is_err() {
        handle_alloc_error(Layout::from_size_align_unchecked(
            len,
            mem::align_of::<A>(),
        ))
    }
    ptr.unwrap().as_ptr() as *mut A
}

pub unsafe fn shrink<A>(ptr:*mut A,old_len:usize,new_len:usize)->*mut A{
    let c = NonNull::new_unchecked(ptr);
    let ptr = Global.shrink(c.cast(),
                             Layout::array::<A>(old_len).unwrap(),
                             Layout::array::<A>(new_len).unwrap());
    if ptr.is_err() {
        handle_alloc_error(Layout::from_size_align_unchecked(
            new_len as usize,
            mem::align_of::<A>(),
        ))
    }
    ptr.unwrap().as_ptr() as *mut A
}

pub unsafe fn drop<A> (ptr:*mut A, len:usize){
    for a in 0..(len as isize) {
        ptr.offset(a).drop_in_place()
    }
    let c: NonNull<A> = NonNull::new_unchecked(ptr);
    Global.deallocate(c.cast(), Layout::array::<A>(len).unwrap());
}

pub unsafe fn grow<A>(ptr:*mut A, old_len:usize,new_len:usize)->*mut A{
    let c = NonNull::new_unchecked(ptr);
    let ptr = Global.grow(c.cast(),
                          Layout::array::<A>(old_len).unwrap(),
                          Layout::array::<A>(new_len).unwrap());
    if ptr.is_err() {
        handle_alloc_error(Layout::from_size_align_unchecked(
            new_len,
            mem::align_of::<A>(),
        ))
    }
    ptr.unwrap().as_ptr() as *mut A
}