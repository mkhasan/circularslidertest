    precision mediump float;

uniform sampler2D u_Texture;

varying vec3 v_Position;
varying vec4 v_Color;

void main()                    
{
	//gl_FragColor = v_Color;

	vec2 coordinate;
	vec4 texture;


	if((v_Position.x >=-1.0) && (v_Position.x <=1.0) && (v_Position.y >=-1.0) && (v_Position.y <=1.0) && (v_Position.z >=0.)) {
		coordinate.x = (v_Position.x+1.0)/2.0;
		coordinate.y = (v_Position.y+1.0)/2.0;
		texture = (texture2D(u_Texture, coordinate));
		texture.a = 0.353;

	}
	else
	    if(v_Position.z >=0.)
		    texture = vec4(1.0,0.0,0.0,1.0);
		else
		    texture = vec4(0.0,0.0,1.0,1.0);



	gl_FragColor = texture;

}                              