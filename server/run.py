#!/usr/bin/env python3

import argparse
import json
import shutil
import subprocess
import os
import re

#colIndex and rowIndex
COL_NUM=0
ROW_NUM=1

#widthIndex and heightIndex
RES_WIDTH=0
RES_HEIGHT=1

#检查是否安装了ffmpeg
def check_environment():
    ffmpeg_path = shutil.which('ffmpeg')
    assert ffmpeg_path is not None, "Cannot find ffmpeg in your $PATH"

#将视频转码且分块，并生成对应的MPD文件（m4s格式）
def transcode(src: str, dst: str, srcVideoSize:list, tiling:list, downsamples:list, bitrates:list):
    tiles_num = tiling[COL_NUM]*tiling[ROW_NUM]
    qualities_num = len(downsamples)
    ffmpeg_filters = []
    src_tile_size=[int(srcVideoSize[COL_NUM]/tiling[COL_NUM]),int(srcVideoSize[ROW_NUM]/tiling[ROW_NUM])]

    #将视频分为若干个视频流，${视频流的数量}=${块的数量}x${分辨率等级}
    for i in range(0,tiling[ROW_NUM]):
        for j in range(0,tiling[COL_NUM]):
            filter = "[0:v]crop=%d:%d:%d:%d[out%d]" % (src_tile_size[COL_NUM], src_tile_size[ROW_NUM], j*src_tile_size[COL_NUM], i*src_tile_size[ROW_NUM], i*tiling[COL_NUM]+j)
            ffmpeg_filters.append(filter)
    for i in range(0,tiles_num):
        filter = "[out%d]split=%d" % (i, len(downsamples))
        for j in range(0,qualities_num):
            filter += "[out%d:%d]" % (i, j)
        ffmpeg_filters.append(filter)
    ffmpeg_filter = ";".join(ffmpeg_filters)


    #一系列复杂参数
    params=[]
    for i in range(0, tiles_num):
        filter=[]
        for j in range(0, qualities_num):
            filter.extend(["-map","[out%d:%d] " % (i, j)])
        filter.extend(["-vcodec","libx264","-preset","medium"])
#        for j in range(0, qualities_num):
#            filter.extend(["-b:v:"+str(i*qualities_num+j), bitrates[j], "-s:v:"+str(i*qualities_num+j), str(int(downsamples[j][COL_NUM]/tiling[COL_NUM]))+"x"+str(int(downsamples[j][ROW_NUM]/tiling[ROW_NUM]))])
        for j in range(0, qualities_num):
            filter.extend(["-s:v:"+str(i*qualities_num+j), str(int(downsamples[j][COL_NUM]/tiling[COL_NUM]))+"x"+str(int(downsamples[j][ROW_NUM]/tiling[ROW_NUM]))])

        filter.extend(["-bf", "16", "-keyint_min", "30", "-g", "30", "-sc_threshold", "0"])
        params.extend(filter)
    params.extend(["-use_timeline", "1", "-use_template", "1", "-seg_duration", "1"])
    # adaptation_sets_params = ["-adaptation_sets", " ".join(adaptation_sets)]
    
    #运行，生成m4s文件，每个视频流有一个init文件和若干个(chunk数量个)数据文件
    command = ["ffmpeg", "-i", src, "-filter_complex", ffmpeg_filter, *params, "-f", "dash", dst]
    print(" ".join(command))
    subprocess.call(command)

#将m4s转为mp4文件
def convertmp4(directory,tiles_num,quality_level):
    files_and_dirs = os.listdir(directory)
    pattern = re.compile(r'chunk-stream0.*m4s')
    files_matching = [f for f in files_and_dirs if pattern.match(f) and os.path.isfile(os.path.join(directory, f))]
    chunkNums=len(files_matching)

    for i in range(0, tiles_num):
        for j in range(0,quality_level):
            streamID=i*quality_level+j
            initName=directory+"init-stream%d.m4s"%streamID
            for chunkID in range(1,chunkNums+1):
                fileName=directory+"chunk-stream%d-%05d.m4s"%(streamID,chunkID)

                temp_output=directory+"tmp.m4s"
                with open(temp_output, 'wb') as wfd:
                    for f in [initName, fileName]:
                        with open(f, 'rb') as fd:
                            wfd.write(fd.read())
                
                ffmpeg_command = ['ffmpeg', '-i', temp_output, '-c', 'copy', directory+"chunk-stream%d-%05d.mp4"%(streamID,chunkID)]
                subprocess.run(ffmpeg_command, check=True)
                os.remove(temp_output)


#重新组织视频流文件，每个分块方案下的分辨率等级创建一个文件夹，简化服务端响应逻辑
def arrangeSource(directory, tiling, downsamples):
    files_and_dirs = os.listdir(directory)
    pattern = re.compile(r'chunk-stream0.*m4s')
    files_matching = [f for f in files_and_dirs if pattern.match(f) and os.path.isfile(os.path.join(directory, f))]
    chunkNums=len(files_matching)
    streamNums = int(tiling[COL_NUM]*tiling[ROW_NUM]) * len(downsamples)

    for i in range(0,len(downsamples)):
        os.mkdir(directory+str(downsamples[i][ROW_NUM]))
    for j in range(0,streamNums):
        shutil.move("%sinit-stream%d.m4s"%(directory, j), "%s/init-tile%d.m4s"%(directory+str(downsamples[j%len(downsamples)][ROW_NUM]), int(j/len(downsamples))+1))

    for j in range(0,streamNums):
        for k in range(1,chunkNums+1):
            shutil.move("%schunk-stream%d-%05d.m4s"%(directory,j,k), "%s/tile%d-chunk%05d.m4s"%(directory+str(downsamples[j%len(downsamples)][ROW_NUM]), int(j/len(downsamples))+1, k))
            shutil.move("%schunk-stream%d-%05d.mp4"%(directory,j,k), "%s/tile%d-chunk%05d.mp4"%(directory+str(downsamples[j%len(downsamples)][ROW_NUM]), int(j/len(downsamples))+1, k))
def main():
    check_environment()

    parser = argparse.ArgumentParser(
    description="This script transcodes source video into DASH-enabled tile-based 360 videos")
    parser.add_argument("input_video", type=str, help="input video path")
    parser.add_argument("input_res", type=str, help="resolution of input videos ($width$x$height$)")
    parser.add_argument("down_samples", type=str, help="list of downsampled resolutions \"[$colums2$x$rows2$, $colums3$x$rows3$, ....]\"")
    parser.add_argument("bitrates", type=str, help="bitrates of downsampled resolutions \"[$rate1$, $rate2$, ....]\"")
    parser.add_argument("tiling", type=str, help="tiling scheme ($colums$x$rows$)")
    parser.add_argument("output_mpd", type=str, help="output MPD path")
    args = parser.parse_args()
    srcVideoSize=[int(args.input_res.split('x')[RES_WIDTH]),int(args.input_res.split('x')[RES_HEIGHT])]
    tiling=[int(args.tiling.split('x')[COL_NUM]),int(args.tiling.split('x')[ROW_NUM])]
    values_list = args.down_samples.strip("[]").strip(" ").split(',')
    downsamples=[]
    for i in range(len(values_list)):
        downsamples.append([int(values_list[i].split('x')[RES_WIDTH]),int(values_list[i].split('x')[RES_HEIGHT])])
    bitrates=args.bitrates.strip("[]").strip(" ").split(',')
    directory, filename = os.path.split(args.output_mpd)
    directory="./" if directory=="" else directory+"/"

    print(tiling)
    print(downsamples)
    print(bitrates)
    #将视频转码且分块，并生成对应的MPD文件（m4s格式）
    transcode(args.input_video, args.output_mpd, srcVideoSize, tiling, downsamples, bitrates)
    #将m4s转为mp4文件
    convertmp4(directory, int(tiling[COL_NUM]*tiling[ROW_NUM]), len(downsamples))
    #重新组织视频流文件，每个分块方案下的分辨率等级创建一个文件夹，简化服务端响应逻辑
    arrangeSource(directory, tiling, downsamples)
if __name__ == '__main__':
    main()
