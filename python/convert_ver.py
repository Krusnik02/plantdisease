from keras import models
import img_helper as imh
from model_helper import ModelHelper







# model params
model_name = 'pomidoro'
input_path = 'E:\\AI\\PlantDisease\\models\\plant_type\\'
output_path = input_path




# read model
model = models.load_model(input_path + model_name)
print('Модель загружена')

# save model
mdh = ModelHelper((128,128,3), 0,0,0)
mdh.export_model(model, model_name, output_path)
mdh.print_model_inputoutput(model,"Модель")