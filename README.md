# beanhold
Example simulation for DEVS DMF using the PHOLD algorithm for parallel computing

This project is an example simulation executing the DEVS DMF framework available at https://github.com/rkewley/devsdmf

The steps completed to get a working simulation:

1.  Clone the devsdmf project, compile it, and create a jar archive using sbt with "sbt package."
2.  Create a new SBT project.
3.  Add the devsdmf jar file created in step 1 to the lib folder of the new SBT project.
4.  A DEVS DMF model is built from the bottom up.  First creat the BeanSimulator, which is a subclass of ModelSimulator.  Note the creation of the following:
    a.  BeanProperties, the static properties of a BeanModel.
    b.  BeanOutData and InitialBeanJumpData, the event messages handled by the BeanSimulator
    c.  BeanState, the state of the BeanSimulator
    d.  BeanStateManager, the manager to record and manage the state of the BeanSimulator
    
    NOTE:  As DEVSDMF matures, we will be able to code generate the BeanSimulator for information abouot the properties, state, and handled messages.
5.  Create BeansModelImpl.  This file is where the event handling code is written to handle the external events that are passed to the BeanSimulator.
6.  Create BeansCoordinator.   This subclass of ModelCoordinator is the equivalent of a DEVS coupled model.  It handles the creation of BeanSimulators on the grid at initialization.  When a BeanSimulator has a bean jump out, the BeanCoordinator receives the message from its subordinate BeanSimulator and routes is appropriately to the new jump destination.
7.  Create BeanRoot, the top level coordinator for the simulation.
8.  Create BeanSimulation with creates a FileLogger and a SimLogger to log simulation events.  It then creates the BeanSimulation and starts execution.


