package com.team1701.robot;

import java.util.Optional;
import java.util.stream.Stream;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.util.PathPlannerLogging;
import com.team1701.lib.cameras.PhotonCameraIO;
import com.team1701.lib.cameras.PhotonCameraIOPhotonCamera;
import com.team1701.lib.drivers.encoders.EncoderIO;
import com.team1701.lib.drivers.encoders.EncoderIOAnalog;
import com.team1701.lib.drivers.gyros.GyroIO;
import com.team1701.lib.drivers.gyros.GyroIOPigeon2;
import com.team1701.lib.drivers.gyros.GyroIOSim;
import com.team1701.lib.drivers.motors.MotorIO;
import com.team1701.lib.util.GeometryUtil;
import com.team1701.robot.Configuration.Mode;
import com.team1701.robot.commands.AutonomousCommands;
import com.team1701.robot.estimation.PoseEstimator;
import com.team1701.robot.subsystems.drive.Drive;
import com.team1701.robot.subsystems.drive.DriveMotorFactory;
import com.team1701.robot.subsystems.drive.SwerveModuleIO;
import com.team1701.robot.subsystems.vision.Vision;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.LoggedDashboardChooser;

import static com.team1701.lib.commands.NamedCommands.*;
import static com.team1701.robot.commands.DriveCommands.*;

public class RobotContainer {
    public final Drive mDrive;
    public final Vision mVision;

    private final CommandXboxController mDriverController = new CommandXboxController(0);
    private final LoggedDashboardChooser<Command> autonomousModeChooser = new LoggedDashboardChooser<>("Auto Mode");

    public RobotContainer() {
        Optional<Drive> drive = Optional.empty();
        Optional<Vision> vision = Optional.empty();

        if (Configuration.getMode() != Mode.REPLAY) {
            switch (Configuration.getRobot()) {
                case SWERVE_BOT:
                    drive = Optional.of(new Drive(new GyroIOPigeon2(10), new SwerveModuleIO[] {
                        new SwerveModuleIO(
                                DriveMotorFactory.createDriveMotorIOSparkMax(10),
                                DriveMotorFactory.createSteerMotorIOSparkMax(11),
                                new EncoderIOAnalog(0)),
                        new SwerveModuleIO(
                                DriveMotorFactory.createDriveMotorIOSparkMax(12),
                                DriveMotorFactory.createSteerMotorIOSparkMax(13),
                                new EncoderIOAnalog(1)),
                        new SwerveModuleIO(
                                DriveMotorFactory.createDriveMotorIOSparkMax(16),
                                DriveMotorFactory.createSteerMotorIOSparkMax(17),
                                new EncoderIOAnalog(3)),
                        new SwerveModuleIO(
                                DriveMotorFactory.createDriveMotorIOSparkMax(14),
                                DriveMotorFactory.createSteerMotorIOSparkMax(15),
                                new EncoderIOAnalog(2)),
                    }));
                    break;
                case SIMULATION_BOT:
                    var gyroIO = new GyroIOSim(
                            () -> PoseEstimator.getInstance().getPose2d().getRotation());
                    var simDrive = new Drive(
                            gyroIO,
                            Stream.generate(() -> SwerveModuleIO.createSim(DCMotor.getKrakenX60(1), DCMotor.getNEO(1)))
                                    .limit(Constants.Drive.kNumModules)
                                    .toArray(SwerveModuleIO[]::new));
                    gyroIO.setYawSupplier(
                            () -> simDrive.getVelocity().omegaRadiansPerSecond, Constants.kLoopPeriodSeconds);
                    drive = Optional.of(simDrive);
                    break;
                default:
                    break;
            }

            vision = Optional.of(new Vision(
                    new PhotonCameraIOPhotonCamera(Constants.Vision.kFrontLeftCameraName),
                    new PhotonCameraIOPhotonCamera(Constants.Vision.kFrontRightCameraName),
                    new PhotonCameraIOPhotonCamera(Constants.Vision.kBackLeftCameraName),
                    new PhotonCameraIOPhotonCamera(Constants.Vision.kBackRightCameraName)));
        }

        this.mDrive = drive.orElseGet(() -> new Drive(
                new GyroIO() {},
                Stream.generate(() -> new SwerveModuleIO(new MotorIO() {}, new MotorIO() {}, new EncoderIO() {}))
                        .limit(Constants.Drive.kNumModules)
                        .toArray(SwerveModuleIO[]::new)));

        this.mVision = vision.orElseGet(() -> new Vision(
                new PhotonCameraIO() {}, new PhotonCameraIO() {}, new PhotonCameraIO() {}, new PhotonCameraIO() {}));

        var teleopTrigger = new Trigger(DriverStation::isTeleopEnabled);
        teleopTrigger.onTrue(runOnce(
                "ZeroGyroscopeToPose",
                () -> mDrive.zeroGyroscope(
                        PoseEstimator.getInstance().getPose2d().getRotation())));

        setupControllerBindings();
        setupAutonomous();
    }

    private void setupControllerBindings() {
        mDrive.setDefaultCommand(driveWithJoysticks(
                mDrive,
                () -> -mDriverController.getLeftY(),
                () -> -mDriverController.getLeftX(),
                () -> -mDriverController.getRightX(),
                () -> mDriverController.leftBumper().getAsBoolean()
                        ? Constants.Drive.kSlowKinematicLimits
                        : Constants.Drive.kFastKinematicLimits));
        mDriverController
                .x()
                .onTrue(runOnce(
                        "ZeroGyroscopeToHeading",
                        () -> mDrive.zeroGyroscope(
                                Configuration.getAlliance().equals(Alliance.Blue)
                                        ? GeometryUtil.kRotationIdentity
                                        : GeometryUtil.kRotationPi)));
        mDriverController.leftTrigger().whileTrue(swerveLock(mDrive));
    }

    private void setupAutonomous() {
        var poseEstimator = PoseEstimator.getInstance();
        AutoBuilder.configureHolonomic(
                poseEstimator::getPose2d,
                poseEstimator::setPose,
                mDrive::getVelocity,
                mDrive::setVelocity,
                Constants.Drive.kPathFollowerConfig,
                mDrive);

        PathPlannerLogging.setLogTargetPoseCallback(pose -> Logger.recordOutput("PathPlanner/TargetPose", pose));
        PathPlannerLogging.setLogActivePathCallback(
                poses -> Logger.recordOutput("PathPlanner/Path", poses.toArray(Pose2d[]::new)));

        var commands = new AutonomousCommands(mDrive);
        autonomousModeChooser.addDefaultOption("Demo", commands.demo());
    }

    public Optional<Command> getAutonomousCommand() {
        return Optional.ofNullable(autonomousModeChooser.get());
    }
}