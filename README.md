# Tremble turn

The main aim of this application is to direct the delivery executives throughout the route made available on Maps without having them to stop and check for turns on the phone. It uses a MCU controlled bands which will vibrate when a turn on the route approaches. 

The application provides following features

- User Authentication via Google sign in
- Searching for destination and providing a path towards it.
 - Communication with the MCU controlled band.
- The application also utilises the feature of pwm possible on the bands. There are three levels of vibration. 
- The highest pwm indicates a steep turn
- The lowest will stop the vibration
- The mid range pwm will indicate a ramp-left , keep-left,  keep-right or ramp-right movements.
- The application also provides the simulation for the same which will place a marker from source and move towards the destination giving proper signals to the bands.
