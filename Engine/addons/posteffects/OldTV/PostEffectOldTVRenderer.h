#pragma once
/** \file posteffectblurrenderer.h
* \brief post effect component

* Copyright (c) 2011,WebJet Business Division,CYOU
* All rights reserved.
* Date        Ver    Author          Comment
* 2011/12/7   1.0    Qi Song   
*/
#include "graphicsystem/Renderable/QuadRenderer.h"
#include "graphicsystem/base/RenderToTexture.h"

namespace Addons
{
	class PostEffectOldTVRenderer : public Graphic::QuadRenderer
	{
		__DeclareSubClass(PostEffectOldTVRenderer,Graphic::QuadRenderer)

	public:
		PostEffectOldTVRenderer();
		virtual ~PostEffectOldTVRenderer();

		virtual void Setup();
		virtual void RenderObj(const Ptr<Graphic::Renderable>& renderable, Graphic::SurfacePassType surface = Graphic::eCustomized,const Ptr<Graphic::Material>& customizedMat = 0);

		float m_timeX;
		float m_sinTimeX;
		RenderBase::TextureHandle m_noiseVolumeMapHandle;
		RenderBase::TextureHandle m_random3DMapHandle;
		bool m_bRTBinded;
	};
}