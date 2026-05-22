package com.afb.scheduler;

import org.springframework.boot.SpringApplication;

public class TestAppointmentSchedulerApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(AppointmentSchedulerApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
