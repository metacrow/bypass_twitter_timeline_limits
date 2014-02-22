bypass_twitter_timeline_limits
==============================

Workaround for the time/tweet limited main twitter timeline. Extends the timeline back as far as twitter's tweet archives go.

INFO:
Handy program if you only check twitter once in a while and the timeline only goes back X hours, causing you to miss a lot of tweets.

Basic little project i made that uses twitter API(Twitter4j, the java implementation) to get the people you're following, then uses Jsoup to go to their profile page and gets all the tweets (or as far back as you set it to) from there. It also uses twitter's api directly to get the tweets of people you're following who are private. It then compiles the tweets into a timeline and writes them to an html doc.

Required Libraries/All Respective Licences of:
  JSoup http://jsoup.org/
  Twitter4j http://twitter4j.org

LICENSE (informal):
In addition to the included library's license's terms,
You're free to use, edit, or give away this program as you wish with the conditions that you attribute the original program creation to me, and do not charge for said program. If you have any specific usage requests for this program that are not covered above, please contact me.
