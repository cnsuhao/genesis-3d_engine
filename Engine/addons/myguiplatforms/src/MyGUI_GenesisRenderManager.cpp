/*!
	@file
	@author		Albert Semenov
	@date		04/2009
*/
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
#include "graphicsystem/GraphicSystem.h"
#include "rendersystem/RenderSystem.h"
#include "materialmaker/parser/GenesisShaderParser.h"
#include "foundation/io/ioserver.h"

#include "MyGUI_GenesisDataManager.h"
#include "MyGUI_GenesisRenderManager.h"
#include "MyGUI_GenesisTexture.h"
#include "MyGUI_GenesisVertexBuffer.h"
#include "MyGUI_LogManager.h"
#include "MyGUI_GenesisDiagnostic.h"
#include "MyGUI_Gui.h"
#include "MyGUI_GenesisInput.h"



#include "MyGUI_GenesisVertexBufferManager.h"
#include "MyGUI_GenesisTexture.h"
#include "MyGUI_ResourceManager.h"

namespace MyGUI
{
	using namespace Graphic;

	Util::String GenesisRenderManager::s_resourcePath = "";
	Util::String g_shaderName = "gui.shader";

	GenesisRenderManager& GenesisRenderManager::getInstance()
	{
		return *getInstancePtr();
	}
	GenesisRenderManager* GenesisRenderManager::getInstancePtr()
	{
		return static_cast<GenesisRenderManager*>(RenderManager::getInstancePtr());
	}

	GenesisRenderManager::GenesisRenderManager() 
		: mUpdate(false)
		, mIsInitialise(false)
		, mManualRender(false)
		, mCountBatch(0)
		, m_shader(NULL)
		, m_shaderHandle(NULL)
		,m_VertexMgr(NULL)
		,m_TextureMgr(NULL)
	{
#if RENDERDEVICE_D3D9
		mVertexFormat = VertexColourType::ColourARGB;
#elif RENDERDEVICE_OPENGLES
		mVertexFormat = VertexColourType::ColourABGR;
#endif
	}
	GenesisRenderManager::~GenesisRenderManager() 
	{
		m_shader = NULL;
	}

	void GenesisRenderManager::initialise(int width, int height, int bufferWidth, int bufferHeight)
	{
		mIsInitialise = true;
		_checkShader();

		m_VertexMgr  = MyGUI::GenesisVertexBufferMgr::Create();
		m_TextureMgr = MyGUI::GenesisTextureMgr::Create();
		setResolution(width, height, bufferWidth, bufferHeight);
	}

	void GenesisRenderManager::shutdown()
	{
		MYGUI_PLATFORM_ASSERT(mIsInitialise, getClassTypeName() << " is not initialised");
		MYGUI_PLATFORM_LOG(Info, "* Shutdown: " << getClassTypeName());

		destroyAllResources();

		MYGUI_PLATFORM_LOG(Info, getClassTypeName() << " successfully shutdown");
		mIsInitialise = false;

		if (m_VertexMgr)
		{
			n_delete(m_VertexMgr);
			m_VertexMgr = NULL;
		}

		if (m_TextureMgr)
		{
			n_delete(m_TextureMgr);
			m_TextureMgr = NULL;
		}
	}

	IVertexBuffer* GenesisRenderManager::createVertexBuffer()
	{
		return new GenesisVertexBuffer();
	}

	void GenesisRenderManager::destroyVertexBuffer(IVertexBuffer* _buffer)
	{
		delete _buffer;
	}

	void GenesisRenderManager::resetViewSize(int bufferWidth, int bufferHeight)
	{
		if (0 == mResolutionConfig.width)
		{
			mCurrentResolution.width = bufferWidth;
		}
		else
		{
			mCurrentResolution.width = mResolutionConfig.width;
		}

		if (0 == mResolutionConfig.height)
		{
			mCurrentResolution.height = bufferHeight;
		}
		else
		{
			mCurrentResolution.height = mResolutionConfig.height;
		}
		GenesisInputManager::getInstancePtr()->_setScreenSize((float)mCurrentResolution.width, (float)mCurrentResolution.height);
	}

	void GenesisRenderManager::setResolution(int width, int height, int bufferWidth, int bufferHeight)
	{
		mResolutionConfig.set(width, height);
		windowResized(bufferWidth, bufferHeight);
	}

	void GenesisRenderManager::windowResized(int bufferWidth, int bufferHeight)
	{
		resetViewSize(bufferWidth, bufferHeight);
		updateRenderInfo();
		onResizeView(mCurrentResolution);
	}

	void GenesisRenderManager::deviceReseted(int bufferWidth, int bufferHeigh)
	{
		windowResized(bufferWidth, bufferHeigh);

		m_VertexMgr->ResetAllBuffers();
		m_TextureMgr->ReLoadManualTextures();
		ResourceManager::getInstancePtr()->reloadFontResource();
		outofDate();

	}
	void GenesisRenderManager::updateRenderInfo()
	{
		GraphicSystem* gs = GraphicSystem::Instance();
		if (gs)
		{
			mInfo.maximumDepth = -0.5f;//[2012/4/12 zhongdaohuan] 
			mInfo.hOffset = gs->GetHorizontalTexelOffset() / float(mCurrentResolution.width);
			mInfo.vOffset = gs->GetVerticalTexelOffset() / float(mCurrentResolution.height);
			mInfo.aspectCoef = float(mCurrentResolution.height) / float(mCurrentResolution.width);
			mInfo.pixScaleX = 1.0f / float(mCurrentResolution.width);
			mInfo.pixScaleY = 1.0f / float(mCurrentResolution.height);
		}
		outofDate();
	}

	void GenesisRenderManager::doRender(IVertexBuffer* _buffer, ITexture* _texture, size_t _count)
	{
		GraphicSystem* gs = GraphicSystem::Instance();
		if (gs)
		{		
			GenesisTexture* tex = static_cast<GenesisTexture*>(_texture);
			if (tex)
			{
				if (tex->GetTextureHandle().IsValid())
				{
					gs->SetTexture(0,tex->GetTextureHandle());

				}
				else
				{
					return;
				}	
			}
			GenesisVertexBuffer* vb = static_cast<GenesisVertexBuffer*>(_buffer);
			RenderBase::PrimitiveHandle handle = vb->GetPrimitiveHandle();
			if (vb)
			{
				gs->DrawPrimitive(handle, 0, _count, 0 , 0);
			}
		}
	}

	void GenesisRenderManager::begin()
	{
	}

	void GenesisRenderManager::end()
	{
	}

	ITexture* GenesisRenderManager::createTexture(const std::string& _name)
	{
		MapTexture::const_iterator item = mTextures.find(_name);
		MYGUI_PLATFORM_ASSERT(item == mTextures.end(), "Texture '" << _name << "' already exist");

		GenesisTexture* texture = new GenesisTexture(_name, GenesisDataManager::getInstance().getGroup());
		mTextures[_name] = texture;
		return texture;
	}

	void GenesisRenderManager::destroyTexture(ITexture* _texture)
	{
		if (_texture == nullptr) return;

		MapTexture::iterator item = mTextures.find(_texture->getName());
		MYGUI_PLATFORM_ASSERT(item != mTextures.end(), "Texture '" << _texture->getName() << "' not found");

		mTextures.erase(item);
		delete _texture;
	}

	ITexture* GenesisRenderManager::getTexture(const std::string& _name)
	{
		MapTexture::const_iterator item = mTextures.find(_name);
		if (item == mTextures.end())
		{
			//Ogre::TexturePtr texture = (Ogre::TexturePtr)Ogre::TextureManager::getSingleton().getByName(_name);
			//if (!texture.isNull())
			//{
			//	ITexture* result = createTexture(_name);
			//	static_cast<OgreTexture*>(result)->setOgreTexture(texture);
			//	return result;
			//}
			return nullptr;
		}
		return item->second;
	}

	bool GenesisRenderManager::isFormatSupported(PixelFormat _format, TextureUsage _usage)
	{
		return true;//[zongdaohuan]
	}

	void GenesisRenderManager::destroyAllResources()
	{
		for (MapTexture::const_iterator item = mTextures.begin(); item != mTextures.end(); ++item)
		{
			delete item->second;
		}
		mTextures.clear();
	}

#if MYGUI_DEBUG_MODE == 1
	bool GenesisRenderManager::checkTexture(ITexture* _texture)
	{
		for (MapTexture::const_iterator item = mTextures.begin(); item != mTextures.end(); ++item)
		{
			if (item->second == _texture)
				return true;
		}
		return false;
	}
#endif

	const IntSize& GenesisRenderManager::getViewSize() const
	{
		return mCurrentResolution;
	}

	VertexColourType GenesisRenderManager::getVertexFormat()
	{
		return mVertexFormat;
	}

	const RenderTargetInfo& GenesisRenderManager::getInfo()
	{
		return mInfo;
	}

	bool GenesisRenderManager::getManualRender()
	{
		return mManualRender;
	}

	void GenesisRenderManager::setManualRender(bool _value)
	{
		mManualRender = _value;
	}

	size_t GenesisRenderManager::getBatchCount() const
	{
		return mCountBatch;
	}

	void GenesisRenderManager::renderGUI(float time)
	{
		Gui* gui = Gui::getInstancePtr();
		if (gui == nullptr || nullptr == m_shaderHandle)
			return;

		onFrameEvent(time);

		_beforeDraw();

		setManualRender(true);
		onRenderToTarget(this, mUpdate);

		mUpdate = false;
	}
	void GenesisRenderManager::_beforeDraw()
	{
		GraphicSystem* gs = GraphicSystem::Instance();
		gs->SetShaderProgram(*m_shaderHandle);
		gs->SetRenderState(m_shader->GetTech()->GetDefaultPass()->GetRenderStateObject(), 0);
	}
	void GenesisRenderManager::_loadShader()
	{		
		m_shaderHandle = nullptr;
		//static Util::String file_name("mygui.cg");
		Util::StringAtom fileName(s_resourcePath + g_shaderName);//
		if(IO::IoServer::Instance()->FileExists(fileName))
		{
			m_shader = GenesisMaterialMaker::MakeFromShader(fileName);
			if (m_shader)
			{
				m_shaderHandle = const_cast<RenderBase::GPUProgramHandle*>(&m_shader->GetTech(0)->GetDefaultPass()->GetGPUProgramHandle(0));
				//assert(NULL != m_shader);
				//assert(NULL != m_shaderHandle);
				GPtr<RenderBase::RenderStateDesc>& rso = m_shader->GetTech()->GetDefaultPass()->GetRenderStateObject();
				RenderBase::DeviceDepthAndStencilState state;
				state = rso->GetDepthAndStencilState();
				state.m_stencilTwoEnable = false;
				state.m_depthEnable = false;
				state.m_depthWriteMask = false;
				rso->SetDepthAndStencilState(state);
			}
		}
	}

	void GenesisRenderManager::SetResourcePath(const Util::String& path)
	{
		s_resourcePath = path;
	}

} // namespace MyGUI
