#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;
uniform float BloomIntensity = 0.8;

void main(){
    vec4 color = texture2D(DiffuseSampler, texCoord);
    
    // Moonlight Client V2风格的Bloom效果
    // 提取高亮区域
    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec4 bloom = color * max(brightness - 0.7, 0.0) * 2.0;
    
    // 高斯模糊采样 - Moonlight风格的多重采样
    vec4 blur = vec4(0.0);
    float kernel[9] = float[](0.0625, 0.125, 0.0625, 0.125, 0.25, 0.125, 0.0625, 0.125, 0.0625);
    
    for(int i = -1; i <= 1; i++){
        for(int j = -1; j <= 1; j++){
            vec2 offset = vec2(i, j) * oneTexel * 2.0; // Moonlight风格的更大采样范围
            blur += texture2D(DiffuseSampler, texCoord + offset) * kernel[(i+1)*3 + (j+1)];
        }
    }
    
    // 混合Bloom效果 - Moonlight风格的柔和混合
    vec4 finalBloom = blur * BloomIntensity * 0.8;
    gl_FragColor = color + finalBloom;
}