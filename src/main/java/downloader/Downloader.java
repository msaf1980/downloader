package downloader;

import java.io.BufferedReader;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import java.util.regex.Pattern;

import Utils.CustomException;
import Utils.FileUtils;
import Utils.LogCustomFormatter;

enum DownloaderAction { 
	LOAD, DUMP, REMAP, REMAPTEST;
	
	public static DownloaderAction fromString(String name) {
		if(name != null ) {
			try {
				return fromStringWithEx(name);
			} catch(IllegalArgumentException e) {
			}
		}
		return null;
	}
	
	public static DownloaderAction fromStringWithEx(String name) {
		 if(name != null ) {
            return DownloaderAction.valueOf(name.toUpperCase());
	    }
	    return null;
	}
	
	public static boolean contains(String name) {
        for (DirStruct dirStruct : DirStruct.values()) {
           if (dirStruct.name().equalsIgnoreCase(name))
               return true;
        }
        return false;
   }
}

public class Downloader {
	//private static final Logger LOGGER = Logger.getLogger(Downloader.class.getName());
	public static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private static Level log_level = Level.INFO;
	static private FileHandler logFile;
	static private ConsoleHandler logConsole;
        static private LogCustomFormatter customFormatter;

	//only extract links from rooturl and print it (if needed to download)
	static DownloaderAction action = DownloaderAction.LOAD;

	private static void exitError(String message) {
		System.err.println(message);
		System.exit(1);
	}
	//-------------------
	private static void usage() {
		System.err.println("download website");
        System.err.println(" Logging:");
        System.err.println("\t -v | --verbose - Verbose logging");
        System.err.println("\t --debug - Debug logging");
        System.err.println("\t-u | --url URL - Download URL");
        System.err.println("\t--u-g | --url-group URLGROUP - Download URL group (use symbol $ for number (add additional $ for fixed length format number with 0, like $$$ for 001) or letter");
        System.err.println("\t set range with");
        System.err.println("\t  --range LETTER1-LETTER2");
        System.err.println("\t  --range NUM1-NUM2");
        System.err.println("\t  -range NUM1-NUM2-STEP");
        System.err.println(" Reject and exclude from reject");
        System.err.println("\t -r | --reject REJECT_REGEX - Reject URL by regular expression");
        System.err.println("\t --r-i | --reject-i | --reject-case-ins REJECT_REGEX - Reject URL by case-insensitive regular expression");
        System.err.println("\t -e | --exclude REJECT_EXCLUDE_REGEX - Exclude URL  from reject by regular expression");
        System.err.println("\t --e-i | --exclude-i | --exclude-case-ins REJECT_EXCLUDE_REGEX - Exclude URL  from reject by case-insensitiv regular expression");
        System.err.println(" -o | --outdir OUTDIR - output directory");
        System.err.println(" -t | --dirstruct DIRSTRUCT - directory structure (FLAT, FLATDIRS, SITEDIRS, DIRS)");
        System.err.println("\t FLAT - save all files in root dir");
        System.err.println("\t FLATDIRS - save html files in root dir and other files in site subdir");
        System.err.println("\t SITEDIRS - replicate site directory strukture");
        System.err.println("\t DIRS - replicate site directory strukture without append sitename (all sites in root dir");
        System.err.println(" -l | --levelon LEVEL_ON - load level from root sites (by default 1 - load only root url, 2 - root url and up to 1 link, etc)");
        System.err.println(" --l-o | --levelout LEVEL_OUT - load level from external sites (load up to LEVEL_OUT links, by default 0 or 1 - disable load, 2 - load up to 1 link from root urls, etc)");
		System.err.println(" -s | --restrict RESTRICT_LEVEL - set restrict level from root URL dir");
        System.err.println("\t rootdir or 0 - load from root URL dir (by default)");
        System.err.println("\t site or -1 - load from root URL site");
        System.err.println("\t load down to RESTRICT_LEVEL from root URL subdir");
        System.err.println(" Actions:");
		System.err.println("\t --path-remap - remap path after DIRSTRUCT change");
        System.err.println("\t --path-remap-test - test remap path after DIRSTRUCT change");
		System.err.println("\t   --path-regex PATH_REMAP|REPLACE[|FLAGS] - remap file's path by regex (i - case insensitive)");
        System.err.println("\t   --dir-regex DIR_REMAP|REPLACE[|FLAGS] - remap file's dir by regex (i - case insensitive)");
        System.err.println(" -n suburl - generate name from suburl fro url ended with / instead index-N");
        System.err.println(" --threads THREADS - load with threads (by default - 1)");
        System.err.println(" --https-equal Set equality for URL with HTTP and HTTPS  protocol");
        
		System.exit(1);
	}
	//-------------------
	private static boolean cmdlineParse(String[] params, boolean recursieve, URLDownloader url_downloader) {
		int l = params.length;
		if (l == 0)
			usage();
		for (int i = 0; i < l; i++) {
		    try {
		    	switch(params[i]) {
                case "-h":
                case "--help":
                    usage();
                    ;
		    	case "-f":
		    	case "--file":
		    		//if (++i >= params.length) 
		    			//usage();
		    		if (FileUtils.isFile(params[++i]) != 1) {
		    			System.err.println(params[i] + " not found!");
		    		   	return false;
		    		}
		    		ArrayList<String> fparams = new ArrayList<String>();
		    		try (BufferedReader br = Files.newBufferedReader(Paths.get(params[i]), StandardCharsets.UTF_8)) {
		    			String line;
		    			Pattern p = Pattern.compile("^[ \t]*(#|$)", Pattern.UNICODE_CASE);
		    			while ((line = br.readLine()) != null)   {
		    				if (p.matcher(line).lookingAt())
		    					continue;
		    				Collections.addAll(fparams, line.split(" "));
		    			}
	    			} catch (IOException e) {
	    				System.err.println("I/O error: " + e.getMessage());
	    				return false;
	    			}
		    		if (! cmdlineParse(fparams.toArray(new String[fparams.size()]), false, url_downloader))
		    			return false;
		    		break;
		    	case "-v":
		    	case "--verbose":
		    		log_level = Level.FINE;
		    		break;
		    	case "--debug":
		    		log_level = Level.FINEST;
		    		break;
		    	case "-u":
		    	case "--url":
		    		url_downloader.addRootURL(params[++i]);
		    		break;
                case "--u-g":
                case "--url-group":
                    url_downloader.addRootURLGroup(params[++i]);
                    break;
                case "--range":
                    url_downloader.setRootURLGroupRange(params[++i]);
                    break;
		    	case "-r":
		    	case "--reject":
		    		url_downloader.addReject(params[++i]);
		    		break;
		    	case "--r-i":
		    	case "--reject-i":
		    	case "--reject-case-ins":
		    		url_downloader.addRejectInsensitive(params[++i]);
		    		break;
		    	case "-e":
		    	case "--exclude":
		    		url_downloader.addExclude(params[++i]);
		    		break;
		    	case "--e-i":
		    	case "--exclude-i":
		    	case "--exclude-case-ins":
		    		url_downloader.addExcludeInsensitive(params[++i]);
		    		break;
		    	case "-o":
		    	case "--outdir":
		    		url_downloader.setOutdir(params[++i]);
		    		break;
		    	case "-t":
		    	case "--dirstruct":
		    		url_downloader.setDirStruct(params[++i]);
		    		break;
		    	case "-l":
		    	case "--levelon":
		    		url_downloader.setLevelOn(params[++i]);
		    		break;
		    	case "--l-o":
		    	case "--levelout":
		    		url_downloader.setLevelOut(params[++i]);
		    		break;
		    	case "-d":
		    	case "--dump-links":
		    		action = DownloaderAction.DUMP;
		    		break;
                case "-n":
		    	case "--name":
		    		url_downloader.setNameGen(params[++i]);
		    		break;
		    	case "-s":
		    	case "--restrict":
		    		url_downloader.setRestrictSite(params[++i]);
		    		break;
		    	case "--path-remap":
		    		action = DownloaderAction.REMAP;
		    		break;
		    	case "--path-remap-test":
		    		action = DownloaderAction.REMAPTEST;
		    		break;
		    	case "--path-regex":
		    		url_downloader.putPathRewriteRegex(params[++i]);
		    		break;
		    	case "--dir-regex":
		    		url_downloader.putDirRewriteRegex(params[++i]);
		    		break;
                case "--threads":
                    url_downloader.setThreads(params[++i]);
                    break;
                case "--https-equal":
                    url_downloader.setHTTPSEqual();
                    break;
		    	default:
		    		exitError(String.format("Parameter '%s' is incorrect!", params[i]));
		    	}
		    } catch (NumberFormatException e) {
		    	exitError(String.format("Argument '%s %s' must be an integer!", params[i-1], params[i]));
		    } catch (IllegalArgumentException e) {
		    	//check Enum from string conversion
		    	exitError(String.format("Argument '%s %s' is incorrect!", params[i-1], params[i]));
		    } catch (ArrayIndexOutOfBoundsException e) {
		    	exitError(String.format("Argument for parameter '%s' not set!", params[i-1]));
		    }
		}
		return true;
	}
	//-------------------
	public static void loggingConfig(String logfile) throws SecurityException, IOException {
		//LOGGER.setUseParentHandlers(false);
		LogManager.getLogManager().reset();
		
        LOGGER.setLevel(log_level);
        customFormatter = new LogCustomFormatter();
        		
        logConsole = new ConsoleHandler();
        logConsole.setFormatter(customFormatter);
        LOGGER.addHandler(logConsole);
        
        if (logfile != null) {
        	// append to logfile 
        	logFile = new FileHandler(logfile, true);
        	logFile.setFormatter(customFormatter);
        	LOGGER.addHandler(logFile);
        }
	}
	//-------------------
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		URLDownloader url_downloader = new URLDownloader();
				
		if (! cmdlineParse(args, true, url_downloader)) {
			System.exit(1);
		}

		try {
			if (action == DownloaderAction.DUMP)
				loggingConfig(null);
			else
				loggingConfig(FileUtils.pathJoin(url_downloader.getOutdir(), "downloader.log"));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Problems with creating the log files");
		}
		
		LOGGER.info("started");
		try {
			try {
				url_downloader.prepare();
				if (action == DownloaderAction.DUMP)
					url_downloader.dump();
				else if (action == DownloaderAction.REMAP)
					url_downloader.remap(false);
				else if (action == DownloaderAction.REMAPTEST)
					url_downloader.remap(true);
				else
                    url_downloader.run();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CustomException e) {
				LOGGER.severe(e.getMessage());
				System.exit(1);
			}
		} finally {
			url_downloader.close();
		}
		LOGGER.info("done");		

		/*
		if (FileUtils.isDir(outdir) == 0)
			throw new CustomException(outdir + "not a directory!");
		break;
		*/
		
		
	}
	//-------------------
}
