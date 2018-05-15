## PV112 seminar project - The Solar system model


#### How to run

Just run 
```bash
./gradlew run
```

If you want to see the Hanoi towers simulation in the terminal, run
```bash
./gradlew cmdLine
```

#### Models used:

1. 


#### Textures used:

1. 

#### Requirements met:

- [ ] At least 2 lights
- [x] Animations
- [ ] Textures (at least 8 objects, using at least 3 distinct textures)
- [x] Number of objects in the scene (at least 10 objects, at least 3 distinct, at least 8 loaded from file)
- [ ] GUI 
- [ ] Conic lights
- [x] Procedural textures (brick wall)
- [ ] Sounds
- [ ] Collision detection (will be doing?)
- [ ] Light dimming (will be doing?)
- [ ] Toon shading (will be doing?)

#### Controls:

1. Manual mode:
  - Each step consists of selecting the origin stick and the target stick.
  - This can be done using keys `1`, `2` or `3` each representing exactly one stick
  - For example the sequence of keys `1 3` means that the player wants to move one ring from the leftmost stick to the rightmost one.
  - Sequence of two identical numbers (eg. `1 1`) is invalid and will be ignored.
  - The desired sequence can be modified. The sequence of keys `1 2 3 1 3` will result in the valid sequence `3 1`. Similarly the sequence `1 2 2 1` ends up as `2 1`
  - The sequence of numbers is then confirmed with `Enter`.
  - The whole user input can be cleared using `C`.
  - User can switch to the automatic mode using `A`.

2. Automatic mode:
  - The automatic mode continuously tries to solve the problem.
  - The automatic mode can be stopped using `M` which also switches the game into the manual mode. 
  
3. Once the game is finished (all rings are moved to the stick that is different than the origin), it can be reset using `R`.