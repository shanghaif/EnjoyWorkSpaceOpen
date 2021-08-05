package org.yzh.protocol.commons;

/**
 * 中华人民共和国交通运输行业标准
 * 道路运输车辆卫星定位系统终端通信协议
 * @author yezhihao
 * @home https://gitee.com/yezhihao/jt808-server
 */
public interface JT808 {

    int 终端通用应答 = 0x0001;
    int 终端心跳 = 0x0002;
    int 终端注销 = 0x0003;
    int 查询服务器时间 = 0x0004;//2019 new
    int 终端补传分包请求 = 0x0005;//2019 new
    int 终端注册 = 0x0100;
    int 终端鉴权 = 0x0102;//2019 modify
    int 查询终端参数应答 = 0x0104;
    int 查询终端属性应答 = 0x0107;
    int 终端升级结果通知 = 0x0108;
    int 位置信息汇报 = 0x0200;
    int 位置信息查询应答 = 0x0201;
    int 事件报告 = 0x0301;//2019 del
    int 提问应答 = 0x0302;//2019 del
    int 信息点播_取消 = 0x0303;//2019 del
    int 车辆控制应答 = 0x0500;
    int 查询区域或线路数据应答 = 0x0608;//2019 new
    int 行驶记录数据上传 = 0x0700;
    int 电子运单上报 = 0x0701;
    int 驾驶员身份信息采集上报 = 0x0702;//2019 modify
    int 定位数据批量上传 = 0x0704;
    int CAN总线数据上传 = 0x0705;
    int 多媒体事件信息上传 = 0x0800;
    int 多媒体数据上传 = 0x0801;
    int 存储多媒体数据检索应答 = 0x0802;
    int 摄像头立即拍摄命令应答 = 0x0805;
    int 数据上行透传 = 0x0900;
    int 数据压缩上报 = 0x0901;
    int 终端RSA公钥 = 0x0A00;

    int 终端上行消息保留 = 0x0F00 - 0x0FFF;

    int 平台通用应答 = 0x8001;
    int 服务器补传分包请求 = 0x8003;
    int 查询服务器时间应答 = 0x8004;//2019 new
    int 终端注册应答 = 0x8100;

    int 设置终端参数 = 0x8103;
    int 查询终端参数 = 0x8104;
    int 终端控制 = 0x8105;
    int 查询指定终端参数 = 0x8106;
    int 查询终端属性 = 0x8107;
    int 下发终端升级包 = 0x8108;
    int 位置信息查询 = 0x8201;
    int 临时位置跟踪控制 = 0x8202;
    int 人工确认报警消息 = 0x8203;
    int 服务器向终端发起链路检测请求 = 0x8204;//2019 new
    int 文本信息下发 = 0x8300;//2019 modify
    int 事件设置 = 0x8301;//2019 del
    int 提问下发 = 0x8302;//2019 del
    int 信息点播菜单设置 = 0x8303;//2019 del
    int 信息服务 = 0x8304;//2019 del
    int 电话回拨 = 0x8400;
    int 设置电话本 = 0x8401;
    int 车辆控制 = 0x8500;
    int 设置圆形区域 = 0x8600;//2019 modify
    int 删除圆形区域 = 0x8601;
    int 设置矩形区域 = 0x8602;//2019 modify
    int 删除矩形区域 = 0x8603;
    int 设置多边形区域 = 0x8604;//2019 modify
    int 删除多边形区域 = 0x8605;
    int 设置路线 = 0x8606;
    int 删除路线 = 0x8607;
    int 查询区域或线路数据 = 0x8608;//2019 new
    int 行驶记录仪数据采集命令 = 0x8700;
    int 行驶记录仪参数下传命令 = 0x8701;
    int 上报驾驶员身份信息请求 = 0x8702;

    int 多媒体数据上传应答 = 0x8800;

    int 摄像头立即拍摄命令 = 0x8801;
    int 存储多媒体数据检索 = 0x8802;
    int 存储多媒体数据上传 = 0x8803;
    int 录音开始命令 = 0x8804;
    int 单条存储多媒体数据检索上传命令 = 0x8805;
    int 数据下行透传 = 0x8900;
    int 平台RSA公钥 = 0x8A00;

    int 平台下行消息保留 = 0x8F00 - 0x8FFF;
    int 厂商自定义上行消息 = 0xE000 - 0xEFFF;//2019 new
    int 商自定义下行消息 = 0xF000 - 0xFFFF;//2019 new
    //added by wgx
    int 音视频参数下发 = 0x8F03;
    int 拍照指令下发 = 0x8F02;
    int 接收拍照上报消息应答 = 0x8F01;
    //added by wgx
    int 应答终端音视频请求 = 0x0F03;
    int 终端音视频请求 = 0x0F02;
    int 终端拍照上报 = 0x0F01;
    //end wgx

}