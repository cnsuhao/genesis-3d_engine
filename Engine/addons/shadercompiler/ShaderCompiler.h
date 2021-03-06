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


#ifndef SHADERCOMPILER_H_
#define SHADERCOMPILER_H_

#include "util/array.h"
#include "util/keyvaluepair.h"
#include "util/string.h"
#include "util/dictionary.h"
#include "core/refcounted.h"


namespace ShaderProgramCompiler
{
	class ShaderPass;
	class ShaderMarcro;
	class ShadingTemplateSetting;

	class GpuProgramCompiler : public Core::RefCounted
{
	__DeclareAbstractClass(GpuProgramCompiler);

public:

	GpuProgramCompiler();

	virtual ~GpuProgramCompiler();

public:

	virtual void InitCompiler();

	virtual void Close() = 0;

	virtual void Compile(ShaderPass* pPass) = 0;

public:

	struct ShaderMarcroInfo
	{
		ShaderMarcroInfo ()
		{

		}
		ShaderMarcroInfo (const Util::String& n, const Util::String& v) 
			: name(n), value(v) 
		{

		}

		void Clear()
		{
			name.Clear();
			value.Clear();
		}

		bool IsDefault()
		{
			return ( name.IsEmpty() && value.IsEmpty() );
		}

		Util::String name;
		Util::String value;
	};

	typedef Util::Dictionary<uint, Util::Array<IndexT> > Permutation;

	typedef Util::Array<ShaderMarcroInfo> ShaderMarcros;

	typedef Util::Dictionary<uint, ShaderMarcros> ShaderMacroPermutation;

	const Util::Array< ShaderMarcroInfo >& GetMacros() const;

	const Util::String& GetResult() const;

	void SetShaderName(const Util::String& name);

	const Permutation& GetPermutation() const;

	void  CreateAllShaderMacrosPermutation(const Util::Array<Util::String>& marcroName, const GPtr<ShaderMarcro>& pMarcro, bool bTemplate);

	void  CreateBuiltInMarcro(const uint iPass, const Util::Array<Util::String>& customMacros, const ShadingTemplateSetting* pSetting = NULL);

	const Util::String& GetBuiltInMacroName(const IndexT iPass) const;

protected:

	void _BeforeCompile(const ShaderPass* pPass);

	void _AfterCompile(bool bForwardPass);

	void  _CreatePermutation(const Util::Array<Util::String>& marcroName, bool bTemplate);

protected:

	
	ShaderMarcros                                m_Macros;

	Util::String                                 m_sProgramStartString;

	Util::String                                 m_sResultCode;
	
	Util::Dictionary<uint, Util::String>         m_sCompiledCode;
	Util::Dictionary<uint, Util::String>         m_sRegisterBinds;

	Util::String                                 m_sShaderName;

	Permutation                                  m_Permutation;

	ShaderMacroPermutation                       m_SubShaderMarcroPermuation;

	Util::Dictionary<IndexT, Util::String>       m_BuiltInMacro;

};

inline const Util::Array< GpuProgramCompiler::ShaderMarcroInfo >& GpuProgramCompiler::GetMacros() const
{
	return m_Macros;
}

inline const Util::String& GpuProgramCompiler::GetResult() const
{
	return m_sResultCode;
}

inline void GpuProgramCompiler::SetShaderName(const Util::String& name)
{
	m_sShaderName = name;
}

inline const GpuProgramCompiler::Permutation& GpuProgramCompiler::GetPermutation() const
{
	return m_Permutation;
}

inline const Util::String& GpuProgramCompiler::GetBuiltInMacroName(const IndexT iPass) const
{
	return m_BuiltInMacro[iPass];
}

};
#endif//SHADERCOMPILER_H_