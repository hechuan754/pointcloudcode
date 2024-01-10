#基于文件传输
参考csdn:https://blog.csdn.net/kaolagirl/article/details/118113530#

##问题1
app传到电脑端的图片总是较小，不是原图，可能存在图片质量损失

找到原因：调用系统相机去获取data时获取到的只是缩略图，如果想要查看大图，需要将拍照得到的原图则保存到手机中，然后再去读取。
现在解决办法：就是发送的时候不是直接发送图片，而是发送图片保存路径，Python端先读取路径然后再将图片转成字节流接收

##需求2，app保持摄像头开着，每隔固定时间截图上传图片到服务器端解码，解码成功后返回解码结果。
即获取相机实时图像预览，然后实现实时图像采集上传。
参考：
1、利用Android Camera2 的照相机api 实现 实时的图像采集与预览
https://blog.csdn.net/davidwillo/article/details/63688319
2、Android Camera2 全屏预览+实时获取预览帧进行图像处理
https://blog.csdn.net/qq_38743313/article/details/101557079
