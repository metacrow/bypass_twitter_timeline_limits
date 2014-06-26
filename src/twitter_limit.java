import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
	
	private static Map<Long, String[]> timeline;// going to be using HashMap, as i do many insertions and gets, but only one sort
	
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
	private static int numofdays;
	private static void get_Following(boolean isstoredlocally, boolean userloggedin) throws TwitterException, IOException{
		if(!isstoredlocally){
			IDs FriendsIDs = twitter.getFriendsIDs(-1);
			ids = FriendsIDs.getIDs();//fill list with all user ids
		}
		
		else{
			//read the stored following ids
			BufferedReader br = new BufferedReader(new FileReader("following.txt"));
			
			//i know i did this this way for a reason. Faster or something.
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
		
		if(userloggedin){
			//convert ids to screen names
			int start=0;
			int currentchunksize=100;
			long [] distribids;
			
			while (start<ids.length)
			{
				if(ids.length-start<100){//if we have less than 100 followers left to add to userName
		        	currentchunksize=ids.length-start;//the amount of followers left (we've gone thought the user's list of followers in chunks of 100 and have less than 100 left to search)
		        }
				
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
		        /*
		        if(ids.length-start<100){//if we have less than 100 followers left to add to userName
		        	currentchunksize=ids.length-start;//the amount of followers left (we've gone thought the user's list of followers in chunks of 100 and have less than 100 left to search)
		        }
		        else{
		        	currentchunksize=100;//do another chunk of 100
		        }
		         */
			}
		}
		
		else{
			/*this will only be used for when the user wants to directly edit the following list, and we need to convert to
			 * human readable screen names BUT the user isn't logged in
			 * Don't want to do it unless we have to b/c user isn't logged in b/c its slow & costly.
			*/
			for(Long id:ids){
				String url="https://twitter.com/account/redirect_by_id?id=" + id;
				HttpURLConnection con = (HttpURLConnection)(new URL(url).openConnection());
				con.setInstanceFollowRedirects(false);
				con.connect();
				String location = con.getHeaderField("Location");
				following.add(location.substring(20));
			}
		}
	}
	
	//if the user wants store all the people they are following, get the ids from twitter and store
	//TODO should implement some check to prevent user from repeately calling (this uses twitter api)
	public static void refresh_plus_store_following() throws TwitterException, IOException{
		IDs FriendsIDs = twitter.getFriendsIDs(-1);
		ids = FriendsIDs.getIDs();
		BufferedWriter bw = new BufferedWriter(new FileWriter("following.txt"));
		for(int i=0;i<ids.length;i++){
			bw.write(String.valueOf(ids[i]));
			bw.newLine();
		}
		bw.close();
	}
	//to allow the user to edit their following list, convert ids to screen names
	//this only is called on gui start, and only if following is saved, so api calls shouldn't be a problem
	public static List<String> load_and_convert_following(boolean userloggedin) throws TwitterException, IOException{
		get_Following(true,userloggedin);
		return following;
	}
	//after the user has edited their following list, convert screennames back to ids and save
	//NOTE:this has do be done without the API, as the user might edit and save repeatedly, and converting to ids is costly w/ api
	public static void convert_and_save_following(List<String> followingedit) throws IOException{
		//not sure if this is the best way, but only way i've found so far
		BufferedWriter bw = new BufferedWriter(new FileWriter("following.txt"));
		for(String user:followingedit){
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL("https://twitter.com/i/profiles/show/" + user + "/timeline").openStream()));
			String htmlraw = null;
			//catch in case user inputs invalid/protected username
			try {
				htmlraw = in.readLine();
			} catch (FileNotFoundException e) {
				System.out.println(user + " is invalid or protected");
			}
			in.close();
			//this is VERY slow/inefficent. Need a better way
			if(htmlraw!=null){
				htmlraw=htmlraw.substring(htmlraw.indexOf(user+"\\\" data-user-id=\\\"")+user.length()+18);//cut off everything before the id #
				htmlraw=htmlraw.substring(0, htmlraw.indexOf("\\\""));//cut off everything immediately after the id
				bw.write(htmlraw);
				bw.newLine();
			}
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
	
	//MAJOR PART 3/4 *:* getting the tweets
	private static void create_threads_for_tweets(){
		Future<Map<Long, String[]>>[] tasks = new Future[following.size()];
		//24 threads is working right now, and i don't want to mess with dynamically guessing the right number. Besides, real bottleneck is number of simultaneous connections, not cpu
		ExecutorService executor = Executors.newFixedThreadPool(24);
		
		//go through all the screen name of following, get tweets w/ jsoup.
		for(int i = 0; i < following.size(); i++) {
			Callable<Map<Long, String[]>> temp= new getusertweetsthread(following.get(i), twitter,numofdays);//Create thread per following
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
	    			"<style> li {list-style-type: none;} .tweet .time{float: right;} .u-isHiddenVisually{position: relative !important;} </style>"+
	    			"<meta charset=\"utf-8\">");
	    out.newLine();
	    
	    //Take keys from HashMap and sort them in array
	    List<Long> timelinesortedkeys = new ArrayList<Long>(timeline.keySet());
	    //TODO Want to sort using Radix sort (maybe?)
	    Collections.sort(timelinesortedkeys);
	    Collections.reverse(timelinesortedkeys);
	    
	   for(Long key:timelinesortedkeys){
	        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a 'at' d, MMMM yyyy");
	        
	        out.write("<li class=\"stream-item\">" +
	        			"<div class=\"tweet\">");
					     if (!((String[])timeline.get(key))[4].equals("")){//this is a retweet
							out.write(
	        				"<div class=\"context\">" +
	        					"<div class=\"with-icn\">" +
	        						"<span class=\"Icon Icon--retweeted Icon--small\"></span>" +
	        						"<span class=\"js-retweet-text\">" +
	        							((String[])timeline.get(key))[4]+
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
	public static void runwithgui(boolean follwingstored, int numofdays,boolean isuserloggedin) throws IOException, URISyntaxException, TwitterException{
		twitter_limit.numofdays=numofdays;
		get_Following(follwingstored,isuserloggedin);
		//now that we know # of following, we can make an educated guess as to the initial size of the timeline Hashmap
		timeline = new HashMap<Long, String[]>(following.size()*30*numofdays);//sure, 30 seems a good averaging number. i guess. Times numofdays to go back
		create_threads_for_tweets();
		sort_timeline();
	}
	
	//have this for direct shortcut to running
	public static void runWithoutGUI() throws IOException, URISyntaxException, TwitterException{
			Long test=System.currentTimeMillis();
		Oauth_Authentication(false);
		numofdays=2;
		get_Following(false,true);
		//now that we know # of following, we can make an educated guess as to the initial size of the timeline Hashmap
		timeline = new HashMap<Long, String[]>(following.size()*30*numofdays);//sure, 30 seems a good averaging number. i guess. Times numofdays to go back
			Long testthreads=System.currentTimeMillis();
		create_threads_for_tweets();
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