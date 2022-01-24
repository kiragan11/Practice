package TestAPI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

public class AreaServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    //状态码
    private static final HttpResponseStatus OK = HttpResponseStatus.OK;
    private static final HttpResponseStatus BAD_REQUEST = HttpResponseStatus.BAD_REQUEST;
    private static final HttpResponseStatus FORBIDDEN = HttpResponseStatus.FORBIDDEN;
    private static final HttpResponseStatus NOT_FOUND = HttpResponseStatus.NOT_FOUND;

    //http头
    private static final AsciiString CONTENT_TYPE = HttpHeaderNames.CONTENT_TYPE;
    private static final AsciiString CONTENT_LENGTH = HttpHeaderNames.CONTENT_LENGTH;
    private static final AsciiString CONNECTION = HttpHeaderNames.CONNECTION;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        if (msg instanceof FullHttpRequest) {

            FullHttpRequest request = (FullHttpRequest) msg;

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode resNode = mapper.createObjectNode();
            String resJsonStr;


            //解析失败
            if (request.decoderResult().isFailure()) {
                resNode.put("error", "400 Bad Request");
                resJsonStr = mapper.writeValueAsString(resNode);
                sendResponse(ctx, request, BAD_REQUEST, resJsonStr);
                return;
            }

            //非post请求
            if (!request.method().equals(HttpMethod.POST)) {
                resNode.put("error", "403 Forbidden");
                resJsonStr = mapper.writeValueAsString(resNode);
                sendResponse(ctx, request, FORBIDDEN, resJsonStr);
                return;
            }

            //正常
            if (request.uri().equals("/area")) {

                ByteBuf bf = request.content();
                String reqJsonStr = bf.toString(CharsetUtil.UTF_8);


                JsonNode reqNode = mapper.readTree(reqJsonStr);
                double height = reqNode.get("height").asDouble();
                double width = reqNode.get("width").asDouble();
                double area = height * width;

                //处理response
                resNode.put("area", area);
                resJsonStr = mapper.writeValueAsString(resNode);

                //写response
                sendResponse(ctx, request, OK, resJsonStr);

            } else {
                ErrorBean bean = new ErrorBean();
                bean.error = "404 NOT FOUND";
                resJsonStr = mapper.writeValueAsString(bean);

//                resNode.put("error", "404 NOT FOUND");
//                resJsonStr = mapper.writeValueAsString(resNode);
                sendResponse(ctx, request, NOT_FOUND, resJsonStr);
            }
        }
    }

    public static class ErrorBean{
        public String error;

    }

    private void sendResponse(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponseStatus status, String resJsonStr) {

        //创建response
        HttpVersion version = request.protocolVersion();
        ByteBuf bf = Unpooled.wrappedBuffer(resJsonStr.getBytes());
        FullHttpResponse response = new DefaultFullHttpResponse(version, status, bf);

        response.headers().set(CONTENT_TYPE, "text/json; charset=UTF-8");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

        boolean keepAlive = HttpUtil.isKeepAlive(request) && (response.status().code() == 200);

        if (!keepAlive) {

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

        } else {

            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
