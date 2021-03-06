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
    @class Timing::Timer
  
    A timer object is the most basic object for time measurement. More
    advanced timing classes often build on top of Timer.
    
    (C) 2006 Radon Labs GmbH
*/
#include "core/config.h"
#if (__WIN32__ || __XBOX360__)
#include "timing/win360/win360timer.h"
namespace Timing
{
class Timer : public Win360::Win360Timer
{ };
}
#elif __WII__
#include "timing/wii/wiitimer.h"
namespace Timing
{
class Timer : public Wii::WiiTimer
{ };
}
#elif __PS3__
#include "timing/ps3/ps3timer.h"
namespace Timing
{
class Timer : public PS3::PS3Timer
{ };
}
#elif __ANDROID__
#include "timing/android/androidtimer.h"
namespace Timing
{
class Timer : public Android::AndroidTimer
{

};
}
#elif __OSX__
#include "timing/osx/osxtimer.h"
namespace Timing
{
    class Timer : public OSX::OSXTimer
    {
        
    };
}
#else
#error "Timing::Timer not implemented on this platform!"
#endif

