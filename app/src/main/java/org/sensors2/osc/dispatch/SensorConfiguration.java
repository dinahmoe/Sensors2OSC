package org.sensors2.osc.dispatch;

/**
 * Created by thomas on 11.11.14.
 */
public class SensorConfiguration {
	private boolean send;
	private int sensorType;
	private String oscParam;
	private float currentValue [];
    private int dimensions;

	public SensorConfiguration() {

	}

	public boolean sendingNeeded(float value[]) {
		if (!this.send) {
			return false;
		}
        boolean hasChanged = false;
        for (int i = 0; i < this.dimensions; ++i){
            if (Math.abs(value[i] - this.currentValue[i]) != 0){
                hasChanged = true;
            }
        }

        if (!hasChanged) {
           return false;
        }
        for (int i = 0; i < this.dimensions; ++i){
            this.currentValue[i] = value[i];
        }
		return true;
	}

	public void setSend(boolean send) {
		this.send = send;
	}

    public boolean getSend() { return this.send; }

	public int getSensorType() {
		return this.sensorType;
	}

	public String getOscParam() {
		return this.oscParam;
	}

	public void setOscParam(String oscParam) {
		this.oscParam = oscParam;
	}

	public void setSensorType(int sensorType) {
		this.sensorType = sensorType;
	}

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
        this.currentValue = new float[this.dimensions];
        for (int i = 0; i < this.dimensions; ++i){
            this.currentValue[i] = 0.0F;
        }
    }
    public int getDimensions() {
        return dimensions;
    }
}
