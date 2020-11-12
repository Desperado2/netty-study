import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import serialize.SerializeSerializeServiceImpl;
import serialize.SerializeService;

import java.util.List;


public class CustomByteToMessageDecoder extends ByteToMessageDecoder {

            /*
                +---------------------------------------------------------------+
                | 魔数 2byte | 协议版本号 1byte | 序列化算法 1byte | 报文类型 1byte  |
                +---------------------------------------------------------------+
                | 状态 1byte |        保留字段 4byte     |      数据长度 4byte     | 
                +---------------------------------------------------------------+
                |                   数据内容 （长度不定）                          |
                +---------------------------------------------------------------+
         */

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        // 判断ByteBuf可读取字节
        if(byteBuf.readableBytes() < 14){
            return;
        }
        // 标记byteBuf读指针位置
        byteBuf.markReaderIndex();
        // 跳过魔数
        byteBuf.skipBytes(2);
        // 跳过协议版本号
        byteBuf.skipBytes(1);
        byte serializeType = byteBuf.readByte();
        // 跳过报文类型
        byteBuf.skipBytes(1);
        // 跳过状态字段
        byteBuf.skipBytes(1);
        // 跳过保留字段
        byteBuf.skipBytes(4);

        int dataLength = byteBuf.readInt();
        if(byteBuf.readableBytes() < dataLength){
            // 重置 ByteBuf 读指针位置
            byteBuf.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        byteBuf.readBytes(data);
        SerializeService serializeService = getSerializeServiceByType(serializeType);
        Object object = serializeService.deserialize(data);
        if(object != null){
            list.add(byteBuf);
        }
    }

    private SerializeService getSerializeServiceByType(byte serializeType){
        if(serializeType == (byte)1){
            return new SerializeSerializeServiceImpl();
        }
        return null;
    }
}
