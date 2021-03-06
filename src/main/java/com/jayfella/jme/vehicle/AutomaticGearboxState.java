package com.jayfella.jme.vehicle;

import com.jayfella.jme.vehicle.part.Gear;
import com.jayfella.jme.vehicle.part.GearBox;
import com.jayfella.jme.vehicle.part.Wheel;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;

public class AutomaticGearboxState extends BaseAppState {

    private final Vehicle vehicle;
    private GearBox gearBox;

    private boolean isCar;
    private Car car;
    private int wheelCount;

    public AutomaticGearboxState(Vehicle vehicle) {
        this.vehicle = vehicle;

        if (vehicle instanceof Car) {
            isCar = true;
            car = (Car) vehicle;
        }
    }

    @Override
    protected void initialize(Application app) {
        wheelCount = car.getNumWheels();
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        this.gearBox = vehicle.getGearBox();
    }

    @Override
    protected void onDisable() {

    }

    @Override
    public void update(float tpf) {

        // gearboxes speeds are in km/h.

        // limit the reported speed to the max speed.
        // if we don't do this and exceed the max speed of the gearbox, the revs return to zero.
        float speed = Math.min(vehicle.getSpeed(Vehicle.SpeedUnit.KMH), vehicle.getGearBox().getMaxSpeed(Vehicle.SpeedUnit.KMH));

        float revs = 0;

        for (int i = 0; i < gearBox.getGearCount(); i++) {
            Gear gear = gearBox.getGear(i);

            if ( speed > gear.getStart() && speed <= gear.getEnd() ) {
                gearBox.setActiveGearNum(i);

                revs = unInterpolateLinear(speed, gear.getStart(), gear.getEnd());
                break;
            }
        }

        // this should be "if has wheels" or something.
        // we need to calculate "slip" at the same time we calculate the revs.
        // the gearbox is responsible for the "base" revs, and outside interaction can alter that, such as wheel spin.
        if (isCar) {

            float revIncrease = 0;

            for (int i = 0; i < wheelCount; i++) {

                Wheel wheel = car.getWheel(i);

                // how much this wheel is "skidding".
                float skid = 1.0f - wheel.getVehicleWheel().getSkidInfo();
                skid *= 0.4f;
                // the acceleration force being applied to this wheel in 0-1 range.
                float wheelforce = wheel.getAccelerationForce();

                // the acceleration force of the accelerator pedal in 0-1 range.
                float acceleration = car.getAccelerationForce();

                // the amount of force being applied to this wheel as a result of acceleration.
                float totalForce = acceleration * wheelforce;

                // if the wheel is accelerating and slipping, increase the revs.
                // find the range of the current revs vs the max revs.
                float revsLeft = 1.0f - car.getEngine().getRevs();

                // float revRange = unInterpolateLinear((totalForce * skid), revsLeft, 1.0f);
                revIncrease = (totalForce * skid);

                // if (revRange > revIncrease) {
                    // revIncrease = revRange;
                // }

            }

            revs += revIncrease;

            // System.out.println("revs: " + revs + " / " + revIncrease);
            // we should probably figure out why it's exceeding 1.0 than clamp it.
            revs = FastMath.clamp(revs, 0, 1);
            vehicle.getEngine().setRevs(revs);

        }



    }

    private float unInterpolateLinear(float value, float min, float max) {
        return (value - min) / (max - min);
    }

}
