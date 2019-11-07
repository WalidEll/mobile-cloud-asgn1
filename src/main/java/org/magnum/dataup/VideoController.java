package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


@RestController
public class VideoController {
    private static final AtomicLong currentId = new AtomicLong(0L);
    private Map<Long,Video> videos = new HashMap<Long, Video>();
    public Video save(Video entity) {
        checkAndSetId(entity);
        videos.put(entity.getId(), entity);
        return entity;
    }

    private void checkAndSetId(Video entity) {
        if(entity.getId() == 0){
            entity.setId(currentId.incrementAndGet());
        }
    }
    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String base =
                "http://"+request.getServerName()
                        + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
        return base;
    }
    @RequestMapping(value = "/video", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public Video addVideo(@RequestBody Video video) {
        Video respVideo= save(video);
        respVideo.setDataUrl(getDataUrl(respVideo.getId()));
        return respVideo;
    }

    @RequestMapping(value = "/video", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public Collection<Video> getVideos() {
        return videos.values();
    }

    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
    public VideoStatus setVideoData(@PathVariable long id, @RequestParam("data")MultipartFile video, HttpServletResponse responseHeader) throws IOException {
        Video v = videos.get(id);
        if (v != null) {
            try {
                VideoFileManager.get().saveVideoData(v, video.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new VideoStatus(VideoStatus.VideoState.READY);
        } else {
            responseHeader.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }
    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] getVideoData(@PathVariable("id") long id, HttpServletResponse httpResponse) throws Exception {

        Video v = videos.get(id);
        if (v != null) {
            boolean hasVideoContent = VideoFileManager.get().hasVideoData(v);
            if(hasVideoContent)
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                VideoFileManager.get().copyVideoData(v, baos);
                return baos.toByteArray();
            }
        } else {
            httpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        return null;

    }
}
