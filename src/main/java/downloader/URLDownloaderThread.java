/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package downloader;

import Utils.CustomException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author msv
 */
public class URLDownloaderThread implements Callable {
    private URLDownloader url_downloader;
    private String url;
    
    public URLDownloaderThread(URLDownloader url_downloader, String url) {
        this.url_downloader = url_downloader;
        this.url = url;
    }
    
    @Override
    public Boolean call() {
        try {
            return url_downloader.loadURL(url, false);
        } catch (CustomException e) {
            Downloader.LOGGER.severe(e.getMessage());
            url_downloader.setShutdown();
            return false;
        }
    }     
}
