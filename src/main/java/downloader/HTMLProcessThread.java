/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package downloader;

import Utils.CustomException;
import java.util.concurrent.Callable;

/**
 *
 * @author msv
 */
public class HTMLProcessThread implements Callable {
    private URLDownloader url_downloader;
    private URLObject url_obj;
    
    public HTMLProcessThread(URLDownloader url_downloader, URLObject url_obj) {
        this.url_downloader = url_downloader;
        this.url_obj = url_obj;
    }
    
    @Override
    public Boolean call() {
        try {
            return url_downloader.processHTML(url_obj, null);
        } catch (CustomException e) {
            Downloader.LOGGER.severe(e.getMessage());
            url_downloader.setShutdown();
            return false;
        }
    }     
}