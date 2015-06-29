# ImageCropView
ImageCropView crops image by moving image under fixed crop area like instagram and iOS.  
Image zoom in/out is base on [sephiroth74's imageViewZoom](https://github.com/sephiroth74/ImageViewZoom).  
Sample app is base on [aviary SDK sample](https://developers.aviary.com).

## Screenshot
![screenshot](doc/img/screenshot.png)

## Installation
[Android Studio](http://developer.android.com/sdk/index.html)  
Android SDK 21  
Android SDK Build-tools 21.1.2  
(You can change other sdk and build-tools)  

## Usage
##### Maven
	<dependency>
	   <groupId&gt;com.naver.android.helloyako</groupId>
	   <artifactId>imagecropview</artifactId>
	   <version>1.0.0</version>
	</dependency>

##### Gradle
	repositories {
	    mavenCentral()
	}

	dependencies {
	    compile 'com.naver.android.helloyako:imagecropview:1.0.3'
	}

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
