/****************************************************************************
Copyright (c) 2006, Radon Labs GmbH
Copyright (c) 2011-2013,WebJet Business Division,CYOU
 
http://www.genesis-3d.com.cn

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
****************************************************************************/
#pragma once
//------------------------------------------------------------------------------
/**
    @class Win360::Win360Heap
  
    Win32/Xbox360 implementation of the class Memory::Heap. Under Win32,
    the LowFragmentationHeap feature is generally turned on.
*/
#include "core/types.h"
#include "threading/interlocked.h"
#include "threading/criticalsection.h"
#include "util/array.h"
#include "util/list.h"

//------------------------------------------------------------------------------
namespace Win360
{
class Win360Heap
{
public:
    /// static setup method (called by Core::SysFunc::Setup)
    static void Setup();
    /// constructor (name must be static string!)
    Win360Heap(const char* name, size_t initialSize=0, size_t maxSize=0);
    /// destructor
    ~Win360Heap();
    /// get heap name
    const char* GetName() const;
    /// allocate a block of memory from the heap
    void* Alloc(size_t size);
    /// re-allocate a block of memory
    void* Realloc(void* ptr, size_t newSize);
    /// free a block of memory which has been allocated from this heap
    void Free(void* ptr);

    #if NEBULA3_MEMORY_STATS
    /// heap stats structure
    struct Stats
    {
        const char* name;
        int allocCount;
        int allocSize;
    };
    /// gather stats from all existing heaps
    static Util::Array<Stats> GetAllHeapStats();
    /// validate all heaps
    static bool ValidateAllHeaps();
    /// validate the heap (only useful in Debug builds)
    bool ValidateHeap() const;
    /// dump memory leaks from this heap
    void DumpLeaks();
    /// dump memory leaks from all heaps
    static void DumpLeaksAllHeaps();
    /// get the current alloc count
    int GetAllocCount() const;
    /// get the current alloc size
    int GetAllocSize() const;
    /// helper method: generate a mem leak report for provided Windows heap
    static void DumpHeapMemoryLeaks(const char* heapName, HANDLE hHeap);
    #endif

private:
    /// default constructor not allowed
    Win360Heap();

    HANDLE heap;
    const char* name;

    #if NEBULA3_MEMORY_STATS
    int volatile allocCount;
    int volatile allocSize;
    static Threading::CriticalSection*  criticalSection;
    static Util::List<Win360Heap*>* list;
    Util::List<Win360Heap*>::Iterator listIterator;
    #endif
};

//------------------------------------------------------------------------------
/**
*/
inline const char*
Win360Heap::GetName() const
{
    n_assert(0 != this->name);
    return this->name;
}

//------------------------------------------------------------------------------
/**
*/
__forceinline void*
Win360Heap::Alloc(size_t size)
{
    #if NEBULA3_MEMORY_STATS
    Threading::Interlocked::Increment(this->allocCount);
    Threading::Interlocked::Add(this->allocSize, int(size));
    #endif
    void* ptr = Memory::__HeapAlloc16(this->heap, HEAP_GENERATE_EXCEPTIONS, size);
    return ptr;
}

//------------------------------------------------------------------------------
/**
*/
__forceinline void*
Win360Heap::Realloc(void* ptr, size_t size)
{
    #if NEBULA3_MEMORY_STATS
    size_t curSize = Memory::__HeapSize16(this->heap, 0, ptr);
    Threading::Interlocked::Add(this->allocSize, int(size - curSize));
    #endif
    void* newPtr = Memory::__HeapReAlloc16(this->heap, HEAP_GENERATE_EXCEPTIONS, ptr, size);
    return newPtr;
}

//------------------------------------------------------------------------------
/**
*/
__forceinline void
Win360Heap::Free(void* ptr)
{
    n_assert(0 != ptr);
    #if NEBULA3_MEMORY_STATS
    size_t size = Memory::__HeapSize16(this->heap, 0, ptr);
    Threading::Interlocked::Add(this->allocSize, -int(size));
    Threading::Interlocked::Decrement(this->allocCount);
    #endif
    BOOL success = Memory::__HeapFree16(this->heap, 0, ptr);
    n_assert(0 != success);
}

} // namespace Win360Heap
//------------------------------------------------------------------------------
