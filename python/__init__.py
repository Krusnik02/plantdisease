# Python 3.6.0
# tensorflow 1.1.0
# Keras 2.0.4



import os
import warnings
from application import PomidorDiseaseApp
import model_helper as mdhp
warnings.filterwarnings('ignore')

# global variables
epochs = 255
batch_size = 20
tt_split = 0.2
shape = (128, 128, 3)

optimizer = 'sgd'
verbose = 2

# path
os.environ["PATH"] += os.pathsep + 'C:/Program Files (x86)/Graphviz2.38/bin/'


# Запускаем
if __name__ == '__main__':

    # готовим аппликейшин и загружаем данные
    pomidor_app = PomidorDiseaseApp(shape)
    PomidorDiseaseApp.model_helper = mdhp.ModelHelper(shape, optimizer, batch_size, verbose)
    pomidor_app.load()
    pomidor_app.show_rnd_image() # показываем

    # обучаем модель классификации растений
    #model = pomidor_app.train_classify_plant(epochs, tt_split)
    #pomidor_app.model_helper.export_model(model, 'plant_type', os.path.join(pomidor_app.models_path, 'plant_type'))

    # обучаем модели - помидоры - отдельные части
    #pomidor_app.plants['pomidor'].trainmodel_parts(pomidor_app.model_helper, epochs, tt_split, 'mnist_indus')
    #pomidor_app.plants['pomidor'].trainmodel_disease_category(pomidor_app.model_helper, epochs, tt_split, 'ResNet50')
    #pomidor_app.plants['pomidor'].trainmodel_diseases(pomidor_app.model_helper, epochs, tt_split, 'ResNet50')

    # обучаем модели - помидоры целиком с фичами
    pomidor_app.plants['pomidor'].trainmodel_disease_category_features(pomidor_app.model_helper, epochs, tt_split, 'VGG16_concat_after_dence')
    #pomidor_app.plants['pomidor'].trainmodel_disease_features(pomidor_app.model_helper, epochs, tt_split, 'VGG16_concat_after_dence')

    # печатаем выводы сетей
    pomidor_app.plants['pomidor'].report_models_inputoutput(pomidor_app.model_helper)

    # экспортируем
    pomidor_app.plants['pomidor'].export_models(pomidor_app.model_helper, pomidor_app.models_path)




