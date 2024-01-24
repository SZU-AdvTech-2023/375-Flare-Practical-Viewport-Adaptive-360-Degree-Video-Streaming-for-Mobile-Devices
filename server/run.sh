#!/bin/bash
#需要处理的源视频名称，mp4文件。
srcVideos=("video0" "video1" "video2" "video3" "video4")
#源视频的分辨率
declare -A srcRes=(
    ["video0"]="4080x2040"
    ["video1"]="4080x2040"
    ["video2"]="4080x2040"
    ["video3"]="4080x2040"
    ["video4"]="4080x2040"
)
#将源视频转码后的分辨率
resLevel="[3840x1920,2880x1440,1920x960,1440x720,960x480,720x360]"
#分辨率对应的码率
bitLevel="[3000k,2500k,2000k,2500k,1000k,500k]"
#分块方案
tilingSetting=("4x4" "5x5" "6x6")
#以当前路径作为根路径
current_path=$(pwd)
#创建名为videos的文件夹
baseDct=$current_path/videos
if [ ! -d ${baseDct} ]; then
    mkdir -p ${baseDct}
    echo "Base directory created at $baseDct"
fi
#循环处理视频
for video in "${srcVideos[@]}"; do
    #videos文件夹下，创建videos/${videoname}文件夹，用以存储每个视频处理后的文件
    videoRootDct=${baseDct}/${video}
    if [ ! -d $videoRootDct ]; then
        mkdir -p $videoRootDct
        echo "Directory created at $videoRootDct"
    else
        echo "Directory already exists at $videoRootDct, renew it"
        rm -rf $videoRootDct
        mkdir -p $videoRootDct
    fi
    #创建videos/${videoName}/${tileingScheme}文件夹，存储每个切分方案下的视频
    #tips:创建分辨率文件夹的代码在 run,py 中的arrangeSource函数里
    for tile in "${tilingSetting[@]}"; do
        tileDct=$videoRootDct/$tile
        mkdir -p $tileDct
        echo "Creat directory $tileDct"
        #运行 run.py 进行处理，python run.py ${videoName} ${srcVideoResolugions} ${tarVideoResolugions} ${tarVideoBitrates} ${tilingScheme} ${dstMPDFILE}
        python3 run.py $video.mp4 ${srcRes[$srcVideos]} ${resLevel}  ${bitLevel} $tile $tileDct/$video.mpd
    done

done

#mpd文件为预处理好的视频描述文件
#有一些小问题，下一版本修改
mv video.mpd $baseDct/video.mpd