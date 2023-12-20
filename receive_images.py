import socket
import os
import base64

tcp_server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

address = ("58.199.162.63", 1213)  # 绑定IP地址和端口
tcp_server.bind(address)

tcp_server.listen(5)
print("服务器已启动，等待客户端连接")

counter = 1
project_dir = ".\data\img"


while True:
    tcp_client, addr = tcp_server.accept()
    print('客户端连接地址', addr)

    # 接收原图路径长度信息
    image_path_size_data = tcp_client.recv(1024)
    image_path_size_str = image_path_size_data.decode('utf-8').strip()

    if not image_path_size_str:
        continue

    image_path_size = int(image_path_size_str)

    # 接收原图路径信息
    image_path_data = tcp_client.recv(image_path_size)
    image_path = image_path_data.decode('utf-8').strip()

    # 使用相对路径保存在项目目录下
    image_path = os.path.join(project_dir, f"image_{counter}.jpg")

    # 确保目录存在
    # os.makedirs(os.path.dirname(image_path), exist_ok=True)

    # 接收原图文件数据
    data = b''

    while True:
        chunk = tcp_client.recv(8192)
        if not chunk:
            break
        data += chunk
    with open(image_path, 'wb') as file:
        file.write(data)

    print(f"原图保存成功: {image_path}")
    counter += 1

    tcp_client.close()