/****************************************************************************
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

#include "stdneb.h"
#include "packagetool/PackageSystem.h"
#include "packagetool/Package.h"


namespace Pack
{
Package m_Package;


void PackageSystem::SetWarningCallBack(WarningCallBack callBack)
{
	Pack::SetWarningCallBack(callBack);
}

bool PackageSystem::OpenPackage()
{
	return m_Package.Open();
}

void PackageSystem::Close()
{
	m_Package.Close();
}


bool PackageSystem::OpenPackageInAPK(const char* apkpath)
{
	return m_Package.OpenInAPK(apkpath);
}

bool PackageSystem::PackageIsOpened()
{
	return m_Package.IsOpened();
}

bool PackageSystem::GetFileInPackage(const char* fileName, GPtr<IO::Stream> pMemStream)
{
	return m_Package.ReadFile(pMemStream, fileName);
}

bool PackageSystem::GetFileInPackageThreadSafe(const char* fileName, GPtr<IO::Stream> pMemStream)
{
	return m_Package.ReadFileThreadSafe(pMemStream, fileName);
}

bool PackageSystem::IsFileExit(const char* fileName)
{
	return m_Package.IsFileExit(fileName);
}



}