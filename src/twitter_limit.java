import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import twitter4j.IDs;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class twitter_limit {

	private static final String CONSUMER_KEY = "QzaaLqSjVwIRCttdn6LYPg";
	private static final String CONSUMER_SECRET= "Ki2pXAANBC9Z5mMM6WVxVGXn7Pq0n2oWomvSXXn2kZk";

	private static Twitter twitter = new TwitterFactory().getInstance();
	//CONSUMER_KEY, CONSUMER_SECRET only set once here 
	static {
		twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
	}
	
	private static Map<Long, String[]> timeline = new HashMap<Long, String[]>(5000);//Using HashMap, as i do many insertions and gets, but only one sort
	
	//used with Oauth_Authentication
	private static void storeAccessToken(long l, AccessToken accessToken) throws IOException{
			FileWriter fstream = new FileWriter("storeAccessToken.txt");
			BufferedWriter out = new BufferedWriter(fstream);
		    out.write("oauth.accessToken=" +accessToken.getToken());
		    out.newLine();
		    out.write("oauth.accessTokenSecret=" + accessToken.getTokenSecret());
		    out.close();
		  }
	private static AccessToken loadAccessToken() throws IOException{
			BufferedReader br = new BufferedReader(new FileReader("storeAccessToken.txt"));
			String token = br.readLine();
			token=token.substring(token.indexOf("=")+1,token.length());
			String tokenSecret = br.readLine();
			tokenSecret=tokenSecret.substring(tokenSecret.indexOf("=")+1,tokenSecret.length());
			br.close();
			return new AccessToken(token, tokenSecret);
	  	}
	
	//used for GUI to check Oauth stuff
	public static boolean checkforAccessToken(){
		File f = new File("storeAccessToken.txt");
		  if(f.exists()){
			try {
				Oauth_Authentication(false);
			} catch (IOException | URISyntaxException | TwitterException e) {e.printStackTrace();}
			  return true;
		  }
		  else{
			  return false;
		  }
	}
	public static String current_loggedin_user() throws IllegalStateException, TwitterException{
		return twitter.getScreenName();
	}
	
	//MAJOR PART 1/4 *:* Oauth authentication
	public static void Oauth_Authentication(boolean getandsaveuser) throws IOException, URISyntaxException, TwitterException{
		if(getandsaveuser){
			twitter.setOAuthAccessToken(null);//Destroy access token to allow new user to login, if this is the first user this wont do anything
			RequestToken requestToken = twitter.getOAuthRequestToken();
		    AccessToken accessToken = null;
		    String pin = "";
		    while (null == accessToken) {
		    	if (Desktop.isDesktopSupported()) {
		    		Desktop.getDesktop().browse(new URI(requestToken.getAuthorizationURL()));
		    	}
		    	else{
		    		System.out.println("Open the following URL and grant access to your account:");
			    	System.out.println(requestToken.getAuthorizationURL());
		    	}
		    	System.out.print("Enter the PIN(if avalilable) or just hit enter.[PIN]:");
		    	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		    	pin = br.readLine();
		    	try{
		    		if(pin.length() > 0){
		    			accessToken = twitter.getOAuthAccessToken(requestToken, pin);
		    		}
		    		else{
		    			accessToken = twitter.getOAuthAccessToken();
		    		}
		    	} catch (TwitterException te) {
		    		if(401 == te.getStatusCode()){
		    			System.out.println("Unable to get the access token.");
		    		}
		    		else{
		    			te.printStackTrace();
		    		}
		    	}
		    }
		    //persist to the accessToken for future reference.
		    storeAccessToken(twitter.verifyCredentials().getId() , accessToken);
	    }
	    else{
	        AccessToken accessToken = loadAccessToken();
	        twitter.setOAuthAccessToken(accessToken);
	    }
	}

	//MAJOR PART 2/4 *:* get a list of everyone user is following
	private static List<String> following = new ArrayList<String>();
	private static long [] ids;//need this global to store following to .txt
	private static void get_Following(boolean isstoredlocally) throws TwitterException, IOException{
		if(!isstoredlocally){
			IDs FriendsIDs = twitter.getFriendsIDs(-1);
			ids = FriendsIDs.getIDs();//fill list with all user ids
		}
		
		else{
			//read the stored following ids
			BufferedReader br = new BufferedReader(new FileReader("following.txt"));
			
			ArrayList<String> temporary = new ArrayList<String>();
			while (br.ready()){
			    temporary.add(br.readLine());
			}
			br.close();

			ids = new long[temporary.size()];
			for(int i=0;i<temporary.size();i++){
				ids[i]=Long.valueOf(temporary.get(i));
			}
		}
		//convert ids to screen names
		int start=0;
		int currentchunksize=100;
		long [] distribids;
		
		while (start<ids.length)
		{
			distribids = new long [currentchunksize];
			for (int i=0;i<currentchunksize;i++)
			{
				distribids[i]=ids[start];//seperate friend ids into chunks (100 max chunk size)
				start++;
			}
			
			ResponseList<User> userName = twitter.lookupUsers(distribids);
			
	        for (User u : userName) {
	        	following.add(u.getScreenName());
	        }
	        if(ids.length-start<100){//if we have less than 100 followers left to add to userName
	        	currentchunksize=ids.length-start;//the amount of followers left (we've gone thought the user's list of followers in chunks of 100 and have less than 100 left to search)
	        }
	        else{
	        	currentchunksize=100;//do another chunk of 100
	        }
		}
	}
	
	//store all the people user is following in txt file
	//TODO inefficient, b/c GUI has to call twitter to convert the ids to screen name every time it start (if user stores them) BUT if i just save the screen name is someone changes theirs it could mess stuff up
	//		Possible solution, store both ids and screen name side by side in file, GUI can get stored screen names, program uses ids normally.
	public static void store_following() throws TwitterException, IOException{
		get_Following(false);
		BufferedWriter bw = new BufferedWriter(new FileWriter("following.txt"));
		for(int i=0;i<ids.length;i++){
			bw.write(String.valueOf(ids[i]));
			bw.newLine();
		}
		bw.close();
	}
	//check to see if following stored
	public static boolean following_stored(){
		File f = new File("following.txt");
		  if(f.exists()){
			  return true;
		  }
		  else{
			  return false;
		  }
	}
	//return screen names of following for GUI (if stored locally)
	public static List<String> all_stored_following(){
		try {
			get_Following(true);
		} catch (IOException | TwitterException e) {e.printStackTrace();}
		return following;
	}
	
	//MAJOR PART 3/4 *:* getting the tweets
	private static void create_threads_for_tweets(int numofdays){
		Future<Map<Long, String[]>>[] tasks = new Future[following.size()];
		ExecutorService executor = Executors.newFixedThreadPool(24);
		
		//go through all the screen name of following, get tweets w/ jsoup.
		for(int i = 0; i < following.size(); i++) {
			Callable<Map<Long, String[]>> temp= new getusertweetsthread(following.get(i), twitter,2);//Create thread per following
			tasks[i] = executor.submit(temp);
		}
		
		//wait for threads to finish
		//TODO get threads in ORDER they finish, i.e first to finish gets returned first
		for (Future<Map<Long, String[]>> task : tasks) {
			Map<Long, String[]> oneuserstweets;
			try {
				oneuserstweets = task.get();
				timeline.putAll(oneuserstweets);
			} catch (InterruptedException | ExecutionException e) {e.printStackTrace();}
		}
		
		executor.shutdown();
		
		//try using a thread pool instead
		//http://docs.oracle.com/javase/tutorial/essential/concurrency/pools.html
		//http://stackoverflow.com/questions/5688295/the-fastest-way-to-fetch-multiple-web-pages-in-java
	}
	
	//MAJOR PART 4/4 *:* sorting timeline
	private static void sort_timeline() throws NumberFormatException, IOException, URISyntaxException{
		//start html file and writer
		OutputStreamWriter fstream = new OutputStreamWriter(new FileOutputStream("timeline_multi.html"), "UTF-8");
	    BufferedWriter out = new BufferedWriter(fstream);
	    
	    //beginning of html: add the stylesheets
	    out.write("<link rel=\"stylesheet\" href=\"https://abs.twimg.com/a/1392075216/css/t1/rosetta_core.bundle.css\" type=\"text/css\">" +
	    			"<style> li {list-style-type: none;} .tweet .time{float: right;} </style>"+
	    			"<meta charset=\"utf-8\">");
	    out.newLine();
	    
	    //Take keys from HashMap and sort them in array
	    List<Long> timelinesortedkeys = new ArrayList<Long>(timeline.keySet());
	    //TODO Want to sort using Radix sort (maybe?)
	    Collections.sort(timelinesortedkeys);
	    Collections.reverse(timelinesortedkeys);
	    
	   for(Long key:timelinesortedkeys){
	        //Map.Entry pairs = (Map.Entry)it.next();//i have no idea how Iterator works.
	        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a 'at' d, MMMM yyyy");
	        
	        out.write("<li class=\"stream-item\">" +
	        			"<div class=\"tweet\">");
					     if (!((String[])timeline.get(key))[4].equals("")){//this is a retweet
							out.write(
	        				"<div class=\"context\">" +
	        					"<div class=\"with-icn\">" +
	        						"<span class=\"Icon Icon--retweeted Icon--small\"></span>" +
	        						"<span class=\"js-retweet-text\">" +
	        							"Retweeted by <b>"+((String[])timeline.get(key))[4]+"</b>" +
	        						"</span>" +
								"</div>" +
							"</div>");
					        }
			out.write(
					"<div class=\"content\">" +
						"<div class=\"stream-item-header\">" +
							"<a href=\"https://twitter.com/"+((String[])timeline.get(key))[0]+"\">" +
								"<img class=\"avatar\" src=\""+((String[])timeline.get(key))[3]+"\">" +
								"<strong class=\"fullname\">" +
									((String[])timeline.get(key))[1] +
								"</strong>" +
								"<span class=\"username\">" +
									"<s>@</s>" +
									"<b>"+((String[])timeline.get(key))[0]+"</b>" +
								"</span>" +
							"</a>" +
							"<small class=\"time\">" +
								"<a href=\"https://twitter.com"+((String[])timeline.get(key))[5]+"\">" +
									"<span>" +
										sdf.format((Long)key*1000) +
									"</span>" +
								"</a>" +
							"</small>" +
						"</div>" +
						"<p>" +
							((String[])timeline.get(key))[2] +
						"</p>" +
					"</div>" +
			"</div>" +
		"</li>");
	        
	        out.newLine();
	    }
	    out.close();
	    
	    //open the html file
	    URI twitterfile = new URI("timeline_multi.html");
	    Desktop.getDesktop().browse(twitterfile);
	}
	
	//final method, call to run entire process
	public static void runwithgui(boolean follwingstored, int numofdays) throws IOException, URISyntaxException, TwitterException{
		get_Following(follwingstored);
		create_threads_for_tweets(numofdays);
		sort_timeline();
	}
	
	//have this for direct shortcut to running
	public static void runWithoutGUI() throws IOException, URISyntaxException, TwitterException{
			Long test=System.currentTimeMillis();
		Oauth_Authentication(false);
		get_Following(false);
			Long testthreads=System.currentTimeMillis();
		create_threads_for_tweets(2);
			System.out.println("Time of threads: " + (double)(System.currentTimeMillis()-testthreads)/(double)1000 + " seconds");
			Long testwrite=System.currentTimeMillis();
		sort_timeline();
			System.out.println("Time to sort/write file: " + (double)(System.currentTimeMillis()-testwrite)/(double)1000 + " seconds");
			System.out.println("Time of total: " + (double)(System.currentTimeMillis()-test)/(double)1000 + " seconds");
			Runtime runtime = Runtime.getRuntime();
			System.out.println("Used Memory (gb):" + (runtime.totalMemory() - runtime.freeMemory()) / (Math.pow(1024,3)));
	}
	
	public static void main(String [ ] args){
		try {
			runWithoutGUI();
		} catch (IOException | URISyntaxException | TwitterException e) {
			e.printStackTrace();
		}
	}
}