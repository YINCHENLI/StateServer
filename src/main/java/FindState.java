/**
 * @author Yinchen Li
 * Date: July 6th, 2018
 * This FindState class serves as the class that handles the post messages 
 * containing the longitude and latitude to find whether it belongs to a state in U.S.A
 */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//URL mapping to "/FindState"
@WebServlet("/")
public class FindState extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	//a map that use state name as key, a list of positions as values
	private Map<String,List<Position>> statesMap = new HashMap<String,List<Position>>();
	
	//get method that is will not be invoked for test but could be useful in the future
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {	    
	    readJSON();
	    output(response, null);
	}
	/**
	 * post method that takes request and return the state
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //read the states.json file in WEB-INF folder
        readJSON();
        
        String latitude = request.getParameter("latitude");
        
        String longitude = request.getParameter("longitude");

        //error handling
        if (latitude == null || longitude == null) {
            output(response, "Parameter missing!");
            return;
        }
        
        try {
            double x = Double.parseDouble(longitude);
            double y = Double.parseDouble(latitude);
            
            //create a position
            Position inputPos = new Position(x, y);
            String state = findState(inputPos);
            
            //if none state is found
            if (state.equals("none")) {
                output(response, "[" + longitude + ", " + latitude +"]" 
                        + " is not located in the U.S.");
                return;
            }
            //found the state
            output(response, "[" + longitude + ", " + latitude + "]" 
                    + " is located in " + state);

        } catch (NumberFormatException e) {
            //handle wrong format
            output(response, "["+longitude + ", " + latitude +"]"
                                              + " is not correct position!");
        }
    }

	/**
	 * read the JSON file and store all the position in the stateMap
	 */
	protected void readJSON() {
	    JSONParser parser = new JSONParser();

	    ServletContext context = this.getServletContext();
	    String path = context.getRealPath("WEB-INF/states.json");
	    
	    BufferedReader input = null;

        try {
            String stateLine;
            input = new BufferedReader(new FileReader(path));
            
            while ((stateLine = input.readLine()) != null) {
                //using the org.json.simple package
                //for each line, initialize an JSON object
                Object obj = parser.parse(stateLine);
                JSONObject jsonObject = (JSONObject) obj;

                String state = (String) jsonObject.get("state");

                // loop array
                JSONArray bdr = (JSONArray) jsonObject.get("border");
                Iterator<JSONArray> iterator = bdr.iterator();
                
                //Note: here we assume that the positions are in clockwise order,
                //the first position is equal to the last position
                //iterate through positions and store them in a list
                List<Position> positionList = new ArrayList<Position>();
                while (iterator.hasNext()) {
                    JSONArray now = iterator.next();
                    Position pos= new Position((Double)now.get(0), (Double)now.get(1));
                    positionList.add(pos);
                }
                
                //put them in statesMap
                statesMap.put(state, positionList);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
	}

	
	/**
	 * Loop through the statesMap and find where the input position is.
	 * @param inputPos a position which the user post to the server
	 * @return the name of the state
	 */
	private String findState(Position inputPos) {
	    
	    String result = null;
	    
	    //loop through the map
	    for(Map.Entry<String, List<Position>> entry: statesMap.entrySet()) {
	        List<Position> polygon = entry.getValue();

	        if (inThisState(inputPos, polygon)) {
	            result = entry.getKey();
	            break;
	        }
	    }
	    if (result == null) {
	        return "none";
	    }
	    return result;
		
	}
	/**
	 * determine whether a position lies in a specific state
	 * draw a line to the left most extreme and if the intersect is odd, it is inside this polygon
	 * Reference: https://www.geeksforgeeks.org/how-to-check-if-a-given-point-lies-inside-a-polygon/
	 * @param inputPos
	 * @param polygon
	 * @return true when the count is odd, false when the count is even
	 */
	private boolean inThisState(Position inputPos, List<Position> polygon) {
	    int n = polygon.size();
	    
	    //the left most position to draw a line between inputPos and extreme
	    Position extreme = new Position(-Double.MAX_VALUE,inputPos.getY());
	    int count = 0;
	    for(int i = 0; i < n - 1; i++) {
	        Position startPos = polygon.get(i);
	        Position endPos = polygon.get(i + 1);
	        if(doIntersect(startPos, endPos, inputPos, extreme)) {
	            if (orientation(startPos, inputPos, endPos) == 0) {
	                return onSegment(startPos, inputPos, endPos);
	            }
	            count++;
	        }
	    }
	    
	    //odd is inside, even is outside
	    return count % 2 == 1;
	}
	/**
	 * The method that determine whether two segments are intersect
	 * @param start1
	 * @param end1
	 * @param start2
	 * @param end2
	 * @return
	 */
	private boolean doIntersect(Position start1, Position end1, Position start2, Position end2) {
	    // Find the four orientations needed for general and special cases
	    int o1 = orientation(start1, end1, start2);
	    int o2 = orientation(start1, end1, end2);
	    int o3 = orientation(start2, end2, start1);
	    int o4 = orientation(start2, end2, end1);
	    
	    //different orientations means intersect
	    if (o1 != o2 && o3 != o4) {
	        return true;
	    }
	    
	 // Special Cases
	    // start1, end1 and start2 are colinear and start2 lies on segment line1
	    if (o1 == 0 && onSegment(start1, start2, end1)) return true;
	 
	    // start1, end1, and end2 are colinear and end2 lies on segment line1
	    if (o2 == 0 && onSegment(start1, end2, end1)) return true;
	 
	    // start2, end2, and start1 are colinear and start1 lies on segment line2
	    if (o3 == 0 && onSegment(start2, start1, end2)) return true;
	 
	     // start2, end2, and end1 are colinear and end1 lies on segment line2
	    if (o4 == 0 && onSegment(start2, end1, end2)) return true;
	    
	    return false;
	}
	
	/** To find orientation of ordered triplet (p, q, r).
	 * The function returns following values
	 * 0 --> p, q and r are colinear
	 * 1 --> Clockwise
	 * 2 --> Counterclockwise
	 * @param p
	 * @param q
	 * @param r
	 * @return
	 */
	private int orientation(Position p, Position q, Position r){
	    double val = (q.getY() - p.getY()) * (r.getX() - q.getX()) - 
	                 (q.getX() - p.getX()) * (r.getY() - q.getY());
	 
	    if (val == 0.0) {
	        return 0;  // colinear
	    }
	    if (val > 0.0) {
	        return 1; //clockwise
	    }
	    return 2;// counterclock wise
	}
	
	/**
	 * Given three colinear points p, q, r, the function checks if
	 * position q lies on line segment 'pr'
	 * @param p position point p
	 * @param q the point that we want to check
	 * @param r position point r
	 * @return
	 */
	private boolean onSegment(Position p, Position q, Position r) {
	    if (q.getX() <= Math.max(p.getX(), r.getX()) && q.getX() >= Math.min(p.getX(), r.getX()) &&
	        q.getY() <= Math.max(p.getY(), r.getY()) && q.getY() >= Math.min(p.getY(), r.getY())) {
	       return true;
	    }
	 
	    return false;
	}
	/**
	 * return message to the terminal
	 * @param response
	 * @param message
	 * @throws IOException
	 */
	private void output(HttpServletResponse response, String message)
			throws IOException {

		PrintWriter out = response.getWriter();

		if (message != null) {
			out.println(message);
		}
	}
}
