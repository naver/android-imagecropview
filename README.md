# ImageCropView
[![Build Status](https://travis-ci.org/naver/android-imagecropview.svg?branch=master)](https://travis-ci.org/naver/android-imagecropview)  

ImageCropView help you image crop in android platform.  
ImageCropView crops image by moving image under fixed crop area like instagram and iOS.  
Image zoom in/out is base on [sephiroth74's imageViewZoom](https://github.com/sephiroth74/ImageViewZoom).  
Sample app is base on [aviary SDK sample](https://developers.aviary.com).

## Screenshot
![screenshot](doc/img/screenshot.png)

- in [pholar](https://play.google.com/store/apps/details?id=com.naver.android.pholar)

![screenshot](doc/img/pholar.gif)
## Installation
[Android Studio](http://developer.android.com/sdk/index.html)  
Android SDK 23  
Android SDK Build-tools 23.0.2  
(You can change other sdk and build-tools)  

## Usage
##### Gradle
	dependencies {
	    compile 'com.naver.android.helloyako:imagecropview:1.1.1'
	}
	
## Grid Option
#### XML
    <com.naver.android.helloyako.imagecrop.view.ImageCropView
                xmlns:imagecrop="http://schemas.android.com/apk/res-auto"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                imagecrop:setInnerGridMode="on"
                imagecrop:gridInnerStroke="1dp"
                imagecrop:gridInnerColor="#66ffffff"
                imagecrop:setOuterGridMode="on"
                imagecrop:gridOuterStroke="1dp"
                imagecrop:gridOuterColor="#ffffff"/>

#### JAVA
    imageCropView.setGridInnerMode(ImageCropView.GRID_ON);
    imageCropView.setGridOuterMode(ImageCropView.GRID_ON);
                


## Demo
[APK](https://github.com/naver/android-imagecropview/raw/master/apk/app-release.apk)  

![qrcode](doc/img/apk_qrcode.png)

## License
ImageCropView is licensed under the Apache License, Version 2.0.
See [LICENSE](LICENSE.txt) for full license text.

        Copyright (c) 2015 Naver Corp.
        @Author Ohkyun Kim

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

                http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
