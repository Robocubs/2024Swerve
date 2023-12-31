package com.team1701.robot;

import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.ReplanningConfig;
import com.team1701.lib.swerve.ExtendedSwerveDriveKinematics;
import com.team1701.lib.swerve.SwerveSetpointGenerator.KinematicLimits;
import com.team1701.lib.util.LoggedTunableNumber;
import edu.wpi.first.math.geometry.*;
import edu.wpi.first.math.util.Units;
import org.photonvision.PhotonPoseEstimator.PoseStrategy;

public final class Constants {
    public static final double kLoopPeriodSeconds = 0.02;

    public static final class Controls {
        public static final double kDriverDeadband = 0.09;
    }

    public static final class Motors {
        public static final double kMaxNeoRPM = 5676;
        public static final double kMaxKrakenRPM = 6000;
    }

    public static final class Drive {
        protected static final double kL1DriveReduction = (14.0 / 50.0) * (25.0 / 19.0) * (15.0 / 45.0);
        protected static final double kL2DriveReduction = (14.0 / 50.0) * (27.0 / 17.0) * (15.0 / 45.0);
        protected static final double kL3DriveReduction = (14.0 / 50.0) * (28.0 / 16.0) * (15.0 / 45.0);
        protected static final double k16ToothKitReduction = (16.0 / 14.0);
        protected static final double kMk4SteerReduction = 1.0 / 12.8;
        protected static final double kMk4iSteerReduction = 7.0 / 150.0;

        public static final double kOdometryFrequency = 250.0;
        public static final double kTrackWidthMeters;
        public static final double kWheelbaseMeters;
        public static final double kModuleRadius;
        public static final double kWheelRadiusMeters;
        public static final double kMaxVelocityMetersPerSecond;
        public static final double kMaxAngularVelocityRadiansPerSecond;
        public static final double kMaxSteerVelocityRadiansPerSecond;
        public static final double kMinLockVelocityMetersPerSecond = 0.2;
        public static final boolean kDriveMotorsInverted;
        public static final boolean kSteerMotorsInverted;
        public static final double kDriveReduction;
        public static final double kSteerReduction;

        public static final int kNumModules;
        public static final ExtendedSwerveDriveKinematics kKinematics;
        public static final KinematicLimits kUncappedKinematicLimits;
        public static final KinematicLimits kFastKinematicLimits;
        public static final KinematicLimits kSlowKinematicLimits;
        public static final KinematicLimits kFastTrapezoidalKinematicLimits;
        public static final KinematicLimits kSlowTrapezoidalKinematicLimits;

        public static final LoggedTunableNumber kDriveKf = new LoggedTunableNumber("Drive/Module/DriveKf");
        public static final LoggedTunableNumber kDriveKp = new LoggedTunableNumber("Drive/Module/DriveKp");
        public static final LoggedTunableNumber kDriveKd = new LoggedTunableNumber("Drive/Module/DriveKd");
        public static final LoggedTunableNumber kSteerKp = new LoggedTunableNumber("Drive/Module/SteerKp");
        public static final LoggedTunableNumber kSteerKd = new LoggedTunableNumber("Drive/Module/SteerKd");

        public static final LoggedTunableNumber kAbsoluteEncoderFL =
                new LoggedTunableNumber("Drive/Module/AbsoluteEncoderFL", 4.89);
        public static final LoggedTunableNumber kAbsoluteEncoderFR =
                new LoggedTunableNumber("Drive/Module/AbsoluteEncoderFR", 5.15);
        public static final LoggedTunableNumber kAbsoluteEncoderBL =
                new LoggedTunableNumber("Drive/Module/AbsoluteEncoderBL", 2.96);
        public static final LoggedTunableNumber kAbsoluteEncoderBR =
                new LoggedTunableNumber("Drive/Module/AbsoluteEncoderBR", 1.11);

        public static final HolonomicPathFollowerConfig kPathFollowerConfig;

        static {
            double driveMotorMaxRPM;
            double turnMotorMaxRPM;

            switch (Configuration.getRobot()) {
                case SWERVE_BOT:
                    kWheelRadiusMeters = Units.inchesToMeters(2);
                    kTrackWidthMeters = 0.465;
                    kWheelbaseMeters = 0.465;
                    driveMotorMaxRPM = Constants.Motors.kMaxNeoRPM;
                    turnMotorMaxRPM = Constants.Motors.kMaxNeoRPM;
                    kDriveReduction = kL3DriveReduction;
                    kSteerReduction = kMk4iSteerReduction;
                    kDriveMotorsInverted = true;
                    kSteerMotorsInverted = true;
                    kDriveKf.initDefault(0.0002);
                    kDriveKp.initDefault(0.00003);
                    kDriveKd.initDefault(0);
                    kSteerKp.initDefault(1.0);
                    kSteerKd.initDefault(0);
                    break;
                case SIMULATION_BOT:
                    kWheelRadiusMeters = Units.inchesToMeters(2);
                    kTrackWidthMeters = 0.5;
                    kWheelbaseMeters = 0.5;
                    driveMotorMaxRPM = Constants.Motors.kMaxKrakenRPM;
                    turnMotorMaxRPM = Constants.Motors.kMaxNeoRPM;
                    kDriveReduction = kL3DriveReduction * k16ToothKitReduction;
                    kSteerReduction = kMk4iSteerReduction;
                    kDriveMotorsInverted = true;
                    kSteerMotorsInverted = true;
                    kDriveKf.initDefault(0.1);
                    kDriveKp.initDefault(0.6);
                    kDriveKd.initDefault(0);
                    kSteerKp.initDefault(16.0);
                    kSteerKd.initDefault(0);
                    break;
                default:
                    throw new UnsupportedOperationException("No drive configuration for " + Configuration.getRobot());
            }

            kModuleRadius = Math.hypot(kTrackWidthMeters / 2.0, kWheelbaseMeters / 2.0);
            kMaxVelocityMetersPerSecond =
                    Units.rotationsPerMinuteToRadiansPerSecond(driveMotorMaxRPM) * kDriveReduction * kWheelRadiusMeters;
            kMaxAngularVelocityRadiansPerSecond =
                    kMaxVelocityMetersPerSecond / Math.hypot(kTrackWidthMeters / 2.0, kWheelbaseMeters / 2.0);
            kMaxSteerVelocityRadiansPerSecond =
                    Units.rotationsPerMinuteToRadiansPerSecond(turnMotorMaxRPM) * kSteerReduction;

            kKinematics = new ExtendedSwerveDriveKinematics(
                    // Front left
                    new Translation2d(kTrackWidthMeters / 2.0, kWheelbaseMeters / 2.0),
                    // Front right
                    new Translation2d(kTrackWidthMeters / 2.0, -kWheelbaseMeters / 2.0),
                    // Back left
                    new Translation2d(-kTrackWidthMeters / 2.0, kWheelbaseMeters / 2.0),
                    // Back right
                    new Translation2d(-kTrackWidthMeters / 2.0, -kWheelbaseMeters / 2.0));

            kNumModules = kKinematics.getNumModules();

            kUncappedKinematicLimits =
                    new KinematicLimits(kMaxVelocityMetersPerSecond, Double.MAX_VALUE, Double.MAX_VALUE);
            kFastKinematicLimits = new KinematicLimits(
                    kMaxVelocityMetersPerSecond, kMaxVelocityMetersPerSecond / 0.2, Units.degreesToRadians(1000.0));
            kSlowKinematicLimits = new KinematicLimits(
                    kMaxVelocityMetersPerSecond * 0.5,
                    kMaxVelocityMetersPerSecond * 0.5 / 0.2,
                    Units.degreesToRadians(750.0));
            kFastTrapezoidalKinematicLimits = new KinematicLimits(
                    kMaxVelocityMetersPerSecond * 0.8,
                    kMaxVelocityMetersPerSecond * 0.8 / 1.5,
                    kFastKinematicLimits.maxSteeringVelocity());
            kSlowTrapezoidalKinematicLimits = new KinematicLimits(
                    kMaxVelocityMetersPerSecond * 0.4,
                    kMaxVelocityMetersPerSecond * 0.4 / 2.0,
                    kFastKinematicLimits.maxSteeringVelocity());

            kPathFollowerConfig = new HolonomicPathFollowerConfig(
                    new PIDConstants(4.0, 0.0, 0.0),
                    new PIDConstants(2.0, 0.0, 0.0),
                    kMaxVelocityMetersPerSecond * 0.95,
                    kModuleRadius,
                    new ReplanningConfig(),
                    kLoopPeriodSeconds);
        }
    }

    public static final class Vision {
        public static final String kFrontLeftCameraName = "CameraFL";
        public static final Transform3d kRobotToFrontLeftCamPose =
                new Transform3d(new Translation3d(0.3, 0.3, 0.2), new Rotation3d(0, 0, Units.degreesToRadians(45)));

        public static final String kFrontRightCameraName = "CameraFR";
        public static final Transform3d kRobotToFrontRightCamPose =
                new Transform3d(new Translation3d(0.3, -0.3, 0.2), new Rotation3d(0, 0, Units.degreesToRadians(-45)));

        public static final String kBackLeftCameraName = "CameraBL";
        public static final Transform3d kRobotToBackLeftCamPose =
                new Transform3d(new Translation3d(-0.3, 0.3, 0.2), new Rotation3d(0, 0, Units.degreesToRadians(135)));

        public static final String kBackRightCameraName = "CameraBR";
        public static final Transform3d kRobotToBackRightCamPose =
                new Transform3d(new Translation3d(-0.3, -0.3, 0.2), new Rotation3d(0, 0, Units.degreesToRadians(-135)));

        public static final double kMaxPoseAmbiguity = 0.03;
        public static final PoseStrategy kPoseStrategy = PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR;
        public static final PoseStrategy kFallbackPoseStrategy = PoseStrategy.LOWEST_AMBIGUITY;
    }
}
