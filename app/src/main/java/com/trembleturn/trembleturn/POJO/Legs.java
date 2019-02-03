package com.trembleturn.trembleturn.POJO;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
 
import com.google.gson.annotations.SerializedName;

public class Legs implements Parcelable {

	public Distance distance;

	public Duration duration;

	@SerializedName("end_address")
	public String endaddress;

	public EndLocation end_location;

	@SerializedName("start_address")
	public String startaddress;

	public StartLocation start_location;

	public List<Steps> steps;

    public Distance getDistance() {
        return distance;
    }

    public void setDistance(Distance distance) {
        this.distance = distance;
    }

    @Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(this.distance, flags);
		dest.writeParcelable(this.duration, flags);
		dest.writeString(this.endaddress);
		dest.writeParcelable(this.end_location, flags);
		dest.writeString(this.startaddress);
		dest.writeParcelable(this.start_location, flags);
		dest.writeList(this.steps);
	}

	public Legs() {
	}

	protected Legs(Parcel in) {
		this.distance = in.readParcelable(Distance.class.getClassLoader());
		this.duration = in.readParcelable(Duration.class.getClassLoader());
		this.endaddress = in.readString();
		this.end_location = in.readParcelable(EndLocation.class.getClassLoader());
		this.startaddress = in.readString();
		this.start_location = in.readParcelable(StartLocation.class.getClassLoader());
		this.steps = new ArrayList<Steps>();
		in.readList(this.steps, Steps.class.getClassLoader());
	}

	public static final Parcelable.Creator<Legs> CREATOR = new Parcelable.Creator<Legs>() {
		@Override
		public Legs createFromParcel(Parcel source) {
			return new Legs(source);
		}

		@Override
		public Legs[] newArray(int size) {
			return new Legs[size];
		}
	};
}