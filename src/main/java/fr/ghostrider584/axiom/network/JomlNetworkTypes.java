package fr.ghostrider584.axiom.network;

import fr.ghostrider584.axiom.util.JomlConvert;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.joml.*;

import static net.minestom.server.network.NetworkBuffer.*;

public final class JomlNetworkTypes {
	public static final NetworkBuffer.Type<Quaternionfc> JQUATERNION_FC =
			NetworkBufferTemplate.template(QUATERNION, JomlConvert::toFloatArray, JomlConvert::toQuaternionf);

	public static final NetworkBuffer.Type<Quaterniondc> JQUATERNION_DC =
			NetworkBufferTemplate.template(QUATERNION, JomlConvert::toFloatArray, JomlConvert::toQuaterniond);

	public static final NetworkBuffer.Type<Quaternionf> JQUATERNION_F =
			NetworkBufferTemplate.template(QUATERNION, JomlConvert::toFloatArray, JomlConvert::toQuaternionf);

	public static final NetworkBuffer.Type<Quaterniond> JQUATERNION_D =
			NetworkBufferTemplate.template(QUATERNION, JomlConvert::toFloatArray, JomlConvert::toQuaterniond);

	public static final NetworkBuffer.Type<Vector3fc> JVECTOR3_FC =
			NetworkBufferTemplate.template(VECTOR3, JomlConvert::toMinestomVec, JomlConvert::toJomlFloatVec);

	public static final NetworkBuffer.Type<Vector3dc> JVECTOR3_DC =
			NetworkBufferTemplate.template(VECTOR3D, JomlConvert::toMinestomVec, JomlConvert::toJomlDoubleVec);

	public static final NetworkBuffer.Type<Vector3f> JVECTOR3_F =
			NetworkBufferTemplate.template(VECTOR3, JomlConvert::toMinestomVec, JomlConvert::toJomlFloatVec);

	public static final NetworkBuffer.Type<Vector3d> JVECTOR3_D =
			NetworkBufferTemplate.template(VECTOR3D, JomlConvert::toMinestomVec, JomlConvert::toJomlDoubleVec);

	private JomlNetworkTypes() {
	}
}