import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringEscapeUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

class getusertweetsthread implements Callable<Map<Long, String[]>>{ 
	private int days;
	private Map<Long, String[]> timeline;
	private Twitter twitter;
	private String user;

	public getusertweetsthread (String user, Twitter twitter, int days) { 
		this.user=user;
	    this.timeline=new HashMap<Long, String[]>(30*days);//if an average user tweets 30 times a day, initial size is that times days. If 30 is right.
	    this.twitter=twitter;
	    this.days=days;
	  }

	  public Map<Long, String[]> call(){
		//TODO i should probably consider a separate text document with all the info about what html element = what and pass that in to each thread, so i can update it when the ui changes
		long timetogoback=86400*days;
		long forusertweettime = System.currentTimeMillis() / 1000L;
		long askedtime=forusertweettime-timetogoback;
		
		long tweetid=-1;
		
		//first pass of user use basic url
		String url = "https://twitter.com/i/profiles/show/" + user + "/timeline";
		
		Object[] nextmove = gethtmlfromurl(url);
		
		//private account
		if((int)nextmove[1]==1){
			private_account(forusertweettime,askedtime);
			return timeline;
		}

		while(forusertweettime>=askedtime){
			//no tweets or couldn't connect (test for every loop b/c each new url is a new error possibility)
			if((int)nextmove[1]==2 || (int)nextmove[1]==3){
				return timeline;
			}
			
			//normal user, get tweets w/ jsoup
			Document doc = Jsoup.parse((String) nextmove[0]);
			
			//can use .select("input[name=buddyname]") given <input type="hidden" name="buddyname">
			Elements alltweets = doc.getElementsByClass("js-stream-item");
			tweetid=Long.valueOf(doc.select("li.js-stream-item").last().attr("data-item-id"))-1;
				
	    	//actually get the tweets
		    for (Element tweet : alltweets){
		    	//get timestamp
		    	Long tweettime = Long.valueOf(tweet.select("span._timestamp").attr("data-time"));
		    	forusertweettime = tweettime;
		    	//get content
		    	String tweettxt = tweet.select("p.tweet-text").html();
		    	//get direct link to each tweet
		    	String tweetidlink=tweet.select("a.tweet-timestamp").attr("href");
		    	//get avatar url
		    	String avatarurl = tweet.select("img.avatar").attr("src");
		    	//get user profile name
		    	String profilename = tweet.select("strong.fullname").text();
		    	//get user username *cant use String user in case its a retweet
		    	String username = tweet.select("span.username").select("b").text();
		    	//retweet
		    	String retweet = tweet.select("div.context").select("span.js-retweet-text").select("a.js-user-profile-link").text();
		    	
		    	if(forusertweettime>=askedtime){//in case user hasn't tweeted for ages don't get tweets past asked date
		    		timeline.put(tweettime,new String[] {username,profilename,tweettxt,avatarurl,retweet,tweetidlink});
		    	}
		    }
		    //second or more passes, have gotten tweetid, use that
			if(tweetid!=-1){
				url = "https://twitter.com/i/profiles/show/"+user+"/timeline?include_available_features=1&include_entities=1&last_note_ts=0&max_id="+tweetid;
			}
			nextmove = gethtmlfromurl(url);
	    }
		return timeline;
	  }
	  
	  public void private_account(long forusertweettime,long askedtime){
		//if user has protected account
    	//use twitter api to get tweets (instead of jsoup)
		  
		//i do need to call their profile once, to get avatar and profile name
		  /*i could use twitter api, but this saves 2 calls per account
		  	*String avatarurl = twitter.showUser(user).getMiniProfileImageURL();
		    *String profilename = twitter.showUser(user).getName();
		  */
		Document doc = null;
		try {
			doc = Jsoup.parse(new URL("https://twitter.com/"+user).openStream(), "UTF-8", "https://twitter.com/"+user);
		} catch (IOException e1) {
			/*if i can't connect, ill just use default profile name and avatar. not worth retrying*/
			e1.printStackTrace();}
		
    	int curpage=1;
    	Paging page = new Paging (curpage, 200);//page number, number per page ***PAGE IS INEFFICENT, REPLACE
    	while(forusertweettime>=askedtime)
		{
    		List<Status> statuses = null;
		    try {
		    	//note: this will not get retweets. need to use other method and combine w/ this, which i can't find
				statuses = twitter.getUserTimeline(user, page);
			} catch (TwitterException e) {
				if(429 == e.getStatusCode()){
					new Exception("**WHOOPS, RATE LIMITED**").printStackTrace();
					return;
				}
				else{
					e.printStackTrace();
					return;
				}
			}
		    
		    for (Status status : statuses){
		    	//get timestamp
		    	Long tweettime = status.getCreatedAt().getTime()/1000;
		    	forusertweettime = status.getCreatedAt().getTime()/1000;
		    	//get content
		    	String tweettxt = status.getText();
		    	//get direct link to each tweet
		    	String tweetidlink="/" + user + "/status/" + String.valueOf(status.getId());//api doesnt return entire href
		    	//get avatar url
		    	String avatarurl="";
		    	if(doc!=null){
		    		avatarurl = doc.select("img.avatar").attr("src");
		    	}
		    	//get user profile name
		    	String profilename = "";
		    	if(doc!=null){
		    		profilename = doc.select("h1.fullname").select("span.profile-field").text();
		    	}
		    	
		    	if(forusertweettime>=askedtime){//in case user hasn't tweeted for ages don't get tweets past asked date
		    		timeline.put(tweettime,new String[] {user,profilename,tweettxt,avatarurl,"",tweetidlink});
		    	}
		    }
		    curpage++;
		    page.setPage(curpage);
		}
    	return;
	  }
	  
	  public Object[] gethtmlfromurl(String urladd){
		String html = "";
		int rtry=6;
		
		//TODO maybe should use a finally here
		//also, fix this clusterfuck
		
		//try to connect (don't just drop after 1 try)
		while(html.equals("")){
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(new URL(urladd).openStream(), "UTF-8"));
				//get the text from url
				html = in.readLine();
				in.close();
			} catch (FileNotFoundException ef){
				//user account is private
				return new Object[]{"",1};
			} catch (IOException e) {
				if(rtry<=0){
					e.printStackTrace();
					System.out.println("CONNECTION ERROR: COULDN'T CONNECT TO USER " + user+ ". *Probably timeout*");
					return new Object[]{"",3};
				}
				rtry--;
			}
		}
		
		//cut off the json beginning and end
		html=html.substring(html.indexOf("items_html\":")+15, (html.length()-2));
		
		//if user has no tweets
		//TODO fix, not the best
		if(html.length()<50){
			return new Object[]{"",2};
		}
		
		//decode txt with string literals to unicode
		String escapedhtml = StringEscapeUtils.unescapeJava(html);
		
		//return html string
		return new Object[]{escapedhtml,0};
	  }
	  
	}
