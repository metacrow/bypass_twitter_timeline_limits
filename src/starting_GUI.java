import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import twitter4j.TwitterException;


public class starting_GUI {
	public static void main(String[] args) {
		createAndShowGUI(500,540);
	}
	
	private static void createAndShowGUI(int height, int width) {
        JFrame f = new JFrame("Bypass Twitter Timeline Limits");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(height,width);
        try {
			f.add(new Pane(height,width));
		} catch (Exception e) {
			e.printStackTrace();
		}
        f.pack();
        f.setVisible(true);
    }

}

@SuppressWarnings("serial")
class Pane extends JPanel {
	/*TODO
	 * ( ) Organize all these GUI parts in a layout
	 * (X) Show current logged in user, refresh user credentials or change user
	 * (X) Option for user to store their following and use that
	 * ( ) Can manually add to following list, store in separate list from twitter following
	 * (X) Option to get user's following from twitter each run or on request
	 * (X) Option to refresh list of followers if auto-refresh not checked
	 * ( ) Current list of all following and added followers(color code)
	 * ( ) Button at bottom to actually run program, only enabled if suer logged in or user stored list of following
	 * ( ) Amount of time to go back
	 * ( ) For user that want immediate start up w/ no gui, settings file that contains default start up values, running twitter_limit directly uses these
	 */
	
	private int width;
	private int height;
	private boolean isuserloggedin=false;
	//info about what to pass to the final run method
	private boolean followingstoredbool=false;
	private int numberofdays=2;
	//GUI elements that need to be global to be adjusted
	private JButton refreshfollowing = new JButton("Get following from twitter");
	private JButton savefollowing = new JButton("Save list of following");
	private JLabel currentlogin= new JLabel();
	private JTextPane listoffollowing = new JTextPane();
	private JTextField numofdays = new JTextField(2);
	private JButton finalrun = new JButton("Run");
	
	public Pane(int height, int width) throws Exception{
		this.width=width;
		this.height=height;
		if(twitter_limit.following_stored()){
			followingstoredbool=true;
		}
		BoxLayout entirelayout = new BoxLayout(this,BoxLayout.Y_AXIS);
		setLayout(entirelayout);
		
		//START : Setting that affect login
		if(twitter_limit.checkforAccessToken()){
    		currentlogin.setText("You are logged in as " + twitter_limit.current_loggedin_user());
    		isuserloggedin=true;
    	}
		else{
			currentlogin.setText("You are not logged in");
		}
		
		JButton login = new JButton("Log in to twitter");
		login.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
            	try {
					twitter_limit.Oauth_Authentication(true);
					currentlogin.setText("You are logged in as " + twitter_limit.current_loggedin_user());
				} catch (IOException | TwitterException | IllegalStateException | URISyntaxException e1) {e1.printStackTrace();}
            	isuserloggedin=true;
            	refreshfollowing.setEnabled(followingstoredbool && isuserloggedin);
            	finalrun.setEnabled(isuserloggedin || followingstoredbool);
            }
		});
		
		JButton logoff = new JButton("<html>Remove saved log-on<br>credentials file (does<br>NOT affect current session)</html>");
		logoff.setMargin(new java.awt.Insets(1, 2, 1, 2));
		
		logoff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
            	File f = new File("storeAccessToken.txt");
      		  	if(f.exists()){
      		  		f.delete();
      		  	}
            }
		});
		//END
		
		//START: storing or updating following list
		JCheckBox autorefreshfollowing = new JCheckBox("<html>Auto-Get list of following<br> every time program runs</html>");
		
		autorefreshfollowing.setSelected(!followingstoredbool);
		autorefreshfollowing.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
            	followingstoredbool ^= true;
            	refreshfollowing.setEnabled(followingstoredbool && isuserloggedin);
            	finalrun.setEnabled(isuserloggedin || followingstoredbool);
            	}
		});
		
		refreshfollowing.setEnabled(followingstoredbool && isuserloggedin);
		refreshfollowing.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
            	try {
					twitter_limit.store_following();
				} catch (TwitterException | IOException e1) {e1.printStackTrace();}
            }
		});
		
		savefollowing.setEnabled(followingstoredbool);
		savefollowing.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
            	try {
					BufferedWriter bw = new BufferedWriter(new FileWriter("following.txt"));
					List<String> followingedit = Arrays.asList(listoffollowing.getText().split("\n"));
					for(String following:followingedit){
						bw.write(following);
						bw.newLine();
					}
					bw.close();
				} catch (IOException e1) {e1.printStackTrace();	}
            }
        });
		
		//list following & option for user to add new following & option to remove following
		//Basically, give user plain text stored following, user can edit it, and save it
		if(followingstoredbool){
			//TODO temporary
							BufferedReader br = new BufferedReader(new FileReader("following.txt"));
							List<String> following = new ArrayList<String>();
							while (br.ready()){
								following.add(br.readLine());
							}
							br.close();
							
			//List<String> following = twitter_limit.all_stored_following();
			String followingtotext="";
			for(String follow:following){
				followingtotext+=follow + "\n";
			}
			listoffollowing.setText(followingtotext);
		}
		JScrollPane listoffollowingsp = new JScrollPane(listoffollowing);

		//END
		
		//START : Amount of time to go back to get tweets
		JLabel numofdayslabel = new JLabel("<html>Number of days<br>to go back</html>");
		JPanel numofdayspanel = new JPanel();
		numofdayspanel.setLayout(new BoxLayout(numofdayspanel, BoxLayout.X_AXIS));
		numofdayspanel.add(numofdayslabel);
		numofdayspanel.add(numofdays);
		
		numofdays.setText("2");
		numofdays.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
            	numberofdays=Integer.valueOf(numofdays.getText());
            }
		});
		//END
		
		//START : run once everything is set up
		finalrun.setEnabled(isuserloggedin || followingstoredbool);
		finalrun.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
            	try {
					twitter_limit.runwithgui(followingstoredbool,numberofdays);
				} catch (IOException | URISyntaxException | TwitterException e1) {e1.printStackTrace();}
            }
		});
		//END
		
		JPanel refreshpane = new JPanel();
		refreshpane.setLayout(new BoxLayout(refreshpane,BoxLayout.Y_AXIS));
		refreshpane.add(Box.createRigidArea(new Dimension(0,20)));
		refreshpane.add(autorefreshfollowing);
		refreshpane.add(Box.createRigidArea(new Dimension(0,20)));
		refreshfollowing.setMargin(new java.awt.Insets(1, 2, 1, 2));
		refreshpane.add(refreshfollowing);
		refreshpane.add(Box.createRigidArea(new Dimension(0,20)));
		refreshpane.add(savefollowing);
		
		JPanel followinglistpane = new JPanel();
		followinglistpane.setLayout(new BoxLayout(followinglistpane,BoxLayout.Y_AXIS));
		JLabel header = new JLabel("Currently Following:");
		followinglistpane.add(header);
		followinglistpane.add(listoffollowingsp);
		
		//make the Run button double as a loading bar, goes along entire bottom, make a speerate pane w/ GridBagLayout
		//http://stackoverflow.com/questions/7279799/with-grouplayout-how-can-i-align-separate-components-to-each-end-of-one-longer
		
		//defining layout positioning
		JPanel main = new JPanel();
		GroupLayout layout = new GroupLayout(main);
		main.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				      .addComponent(currentlogin)
				      .addComponent(followinglistpane)
				)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(login)
						.addComponent(refreshpane)
						.addComponent(numofdayspanel)
				)
				.addComponent(logoff)
		);
		
		layout.setVerticalGroup(
				layout.createSequentialGroup()
			      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
			    		  .addComponent(currentlogin)
			    		  .addComponent(login)
			    		  .addComponent(logoff)
			    		  )
			      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
			    		  .addComponent(followinglistpane)
			    		  .addComponent(refreshpane)
			    		  )
				  .addComponent(numofdayspanel) 
		);
		add(main);
		add(finalrun);
		
	}
	
	public Dimension getPreferredSize() {
        return new Dimension(width,height);
    }
}
