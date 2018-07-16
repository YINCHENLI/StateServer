#State-Server
-------------------

* Author: Yinchen Li
* Email: careeryinchenli@gmail.com

This small code puzzle is using Tomcat Web Server with JAVA 8.

You will need to use Java 8.


##Steps to run




Open a new terminal, and post messages on port 8080

```
curl -d "longitude=-77.036133&latitude=40.513799" http://localhost:8080/State-Server/FindState
```
It shall return 
```
[-77.036133,40.513799] is located in Pennsylvania
```

If you would like see the code, it is located at webapps directory.



##Java classes

###FindState.java
The logic to determine whether a position is inside a polygon.

1) Draw a horizontal line to the right of each point and extend it to infinity [ in the code, I used the left most extreme longitude as ```-DOUBLE.MAX_VALUE``` as x ]

2) Count the number of times the line intersects with polygon edges.

3) A point is inside the polygon if either count of intersections is odd or
   point lies on an edge of polygon.  If none of the conditions is true, then 
   point lies outside.


Reference: 

<https://www.geeksforgeeks.org/how-to-check-if-a-given-point-lies-inside-a-polygon/>

**Sample picture**

![MacDown Screenshot](https://cdncontribute.geeksforgeeks.org/wp-content/uploads/polygon1.png)

In this class, I implemented different methods which start from posting the message, read json, store in stateMap, count the intersections, find the state, and output the result.
