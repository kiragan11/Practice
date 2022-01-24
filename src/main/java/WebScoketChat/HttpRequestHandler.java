package WebScoketChat;


import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;


public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException {

        //获取首页文件路径
        String indexFilePath =
                HttpRequestHandler.class.getClassLoader().getResource("WebSocketChatClient.html").getPath();

        //随机文件,只读
        File indexFile = new File(indexFilePath);
        RandomAccessFile indexRaf;
        try {
            indexRaf = new RandomAccessFile(indexFile,"r");

        }catch (FileNotFoundException e){
            sendError(ctx,request,HttpResponseStatus.NOT_FOUND);
            return;
        }
        long indexFileLength = indexRaf.length();

        //request解码不成功
        if (!request.decoderResult().isSuccess()){
            sendError(ctx,request,HttpResponseStatus.BAD_REQUEST);
            return;
        }

        //非Get请求
        if (!HttpMethod.GET.equals(request.method())){
            sendError(ctx,request,HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        //indexFile不存在或禁止访问
        if (indexFile.isHidden()||!indexFile.exists()){
            sendError(ctx,request,HttpResponseStatus.NOT_FOUND);
            return;
        }

        //成功发送
        if ("/".equals(request.uri())||"/index.html".equals(request.uri())){
            HttpResponse response =
                    new DefaultHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/html; charset=UTF-8");
            HttpUtil.setContentLength(response,indexFileLength);
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (!keepAlive){
                response.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.CLOSE);
            }else if (request.protocolVersion().equals(HttpVersion.HTTP_1_0)){
                response.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);
            }

            // Write the initial line and the header.
            ctx.write(response);

            // Write the content.
            ChannelFuture sendFileFuture;
            ChannelFuture lastContentFuture;

            if(ctx.pipeline().get(SslHandler.class)==null){
                sendFileFuture = ctx.write(new DefaultFileRegion(
                        indexRaf.getChannel(),0,indexFileLength));//, ctx.newProgressivePromise());
                // Write the end marker.
                lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            }else{
                sendFileFuture = ctx.writeAndFlush(new HttpChunkedInput(
                        new ChunkedFile(indexRaf,0,indexFileLength,8192)));//, ctx.newProgressivePromise());
                // HttpChunkedInput will write the end marker (LastHttpContent) for us.
                lastContentFuture = sendFileFuture;
            }

            //监听器
            sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                    if (total<0){
                        System.err.println(future.channel()+" Transfer progress: " +progress);
                    }else {
                        System.err.println(future.channel()+" Transfer progress: " +progress+"/"+total);

                    }
                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                    System.err.println(future.channel() + " Transfer complete.");
                }
            });

            // Decide whether to close the connection or not.
            if (!keepAlive){
                lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            }

            indexRaf.close();

        }else {
            sendError(ctx,request,HttpResponseStatus.FORBIDDEN);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 发送有问题的response
     * @param ctx
     * @param request
     * @param status 状态码
     */
    private void sendError(ChannelHandlerContext ctx,FullHttpRequest request,HttpResponseStatus status){

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,status, Unpooled.copiedBuffer("Failure: "+status+"\r\n", CharsetUtil.UTF_8));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/html; charset=UTF-8");

        HttpUtil.setContentLength(response,response.content().readableBytes());

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (!keepAlive){
            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.
            response.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.CLOSE);
        }else if(request.protocolVersion().equals(HttpVersion.HTTP_1_0)){
            response.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderValues.KEEP_ALIVE);
        }

        ChannelFuture future = ctx.writeAndFlush(response);

        if (!keepAlive){
            // Close the connection as soon as the response is sent.
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
