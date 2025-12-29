package com.hfad.beeradviser

import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class MyAppGlideModule : AppGlideModule() {
    // 通常不需要覆寫任何方法，只需聲明類別即可觸發生成器
}