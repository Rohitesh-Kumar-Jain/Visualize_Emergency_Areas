# Visualize_Emergency_Areas
Prototype that allows users run simulations and visualize the results on a native android platfrom

## Motivation
* The goal is to develop a prototype that allows users run simulations and visualize the results on a handy platform

* In a scenario where the user is onsite, user needs a portable and easily accessible platform to run and visualize simulations.
  * Emergency response (forest fires, floods, mass casualty situations)
  * Military applications (battlefield simulations)
  * Remote locations (ocean, forest, inhabitable locations like Antarctica)

* This type of simulation could open up novel applications for simulations.

## Why on a mobile Platform
* Mobile platforms have more hardware resources available these days, like chrome limits to 4gb RAM per tab, where has mobile these days have 16gb RAM
* There are about 15 billion mobile devices operating in the world 
* You can alert about critical information via notifications
* A mobile device has a GPS which in some point of time we believe will be important as we are interested in dealing with simulation and visualizations on site, apart from this mobile platform offers camera, accelerometer, compass, etc.

## Project
* A native android application would be developed
* We will be reusing the Web Based Architecture for Geospatial Simulation (WARGS)
* User open up the mobile application
* They set up a simulation experiment
* They run the simulation experiment using the WARGS
* The simulation environment returns simulation results
* The front end application requests geospatial data
* The application matches the results to the geo data, draws and animates the map

## GIS, ArcGIS & ArcGIS Online
* Geographic Information System (GIS) is a system that creates, manages, analyzes, and maps all types of data.
* ArcGIS is a GIS software developed by Esri, it is a set of software which has capabilities to view, edit, manage and analyze geographical data. 
* ArcGIS Online is a cloud based solution to create and share interactive web maps, layers, and geographical data. Data can be published as web layers on ArcGIS online.

## Architecture Diagram
<img width="526" alt="Screenshot 2022-10-20 at 12 18 34 AM" src="https://user-images.githubusercontent.com/62026125/196778260-c89b6c21-01c8-4c5c-ae03-6153133a507e.png">

## Progress


## Next Steps, and Future Work
* Execute simulation from the mobile device, and see results on the mobile application itself.
  * Visualize how the situation might look after a user decided time
  * Give notifications about the warnings early
  * Help user visualize and analyze the possible scenarios on the basis of their custom experiments
* Optimize transmission of results from backend to frontend

### Making application offline Capable
* Idea is to make most of the features of the application functional when the internet is unstable, weak or completely down
* Required steps would be to
  * Run the local services, or use the underlying WARGS API
  * Preload the Geospatial data on the device
  * Business processes would need to be reevaluated
  * Install simulator on the device
  * Test performance
