from flask import Flask, request, send_from_directory, jsonify, make_response
import os

# 运行命令: gunicorn -w 8 -b ${IP}:${PORT} server:app --log-file=- --access-logfile=- --error-logfile=-

app = Flask(__name__)

#媒体文件根目录
FILES_DIRECTORY = '/home/ght/Videos/videos'

#响应mpd文件
@app.route('/mpd', methods=['GET'])
def get_mpd():
    video_path = os.path.join(FILES_DIRECTORY, "video.mpd")
    response = make_response(send_from_directory(directory=os.path.dirname(video_path), 
                                                 path=os.path.basename(video_path), 
                                                 as_attachment=True))
    return response

#响应mp4文件，路径为  FILES_DIRECTORY/${videoNmae}/${tilingScheme}${videoResolution}/tile${tileID}-chunk${chunkID}.mp4
@app.route('/video', methods=['GET'])
def get_video():
    video_name = request.args.get('video_name')
    chunk_id = request.args.get('chunk_id')
    resolution = request.args.get('resolution')
    tiling_scheme = request.args.get('tiling_scheme')
    tile_index = request.args.get('tile_index')

    if not all([video_name, chunk_id, resolution, tiling_scheme, tile_index]):
        return jsonify({"error": "Missing parameters"}), 400

    tiling_parts = tiling_scheme.split(',')
    tiling_col, tiling_row = int(tiling_parts[0]), int(tiling_parts[1])

    locatTile = os.path.join(FILES_DIRECTORY, video_name, 
                             "{}x{}".format(tiling_col, tiling_row),
                             resolution,
                             "tile{}-chunk{:05d}.mp4".format(int(tile_index) + 1, int(chunk_id) + 1))

    response = make_response(send_from_directory(directory=os.path.dirname(locatTile), 
                                                 path=os.path.basename(locatTile), 
                                                 as_attachment=True))
    return response
